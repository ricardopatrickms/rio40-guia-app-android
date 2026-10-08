package br.com.rio40graus.guiascale.rastreio

import android.content.Context
import br.com.rio40graus.guiascale.dados.BancoLocal
import br.com.rio40graus.guiascale.rede.LotePosicoes
import br.com.rio40graus.guiascale.rede.PosicaoEnviada
import br.com.rio40graus.guiascale.rede.Rede
import br.com.rio40graus.guiascale.rede.Sessao
import kotlinx.coroutines.sync.Mutex
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * O ÚNICO lugar que sobe a fila de posições.
 *
 * A fila local (Room) é a fonte da verdade do trajeto: o app SEMPRE grava ali
 * primeiro e nunca manda nada que não tenha passado por ela. Subir é só esvaziar
 * essa fila — e é isto que este objeto faz, nas duas situações que precisam
 * disso:
 *
 *  - AO VIVO: o serviço chama a cada captura (~8s), direto e em processo, para o
 *    painel ver o guia se mover na hora.
 *  - REDE DE SEGURANÇA: o EnvioWorker chama pelo WorkManager, que sobrevive ao
 *    app ser fechado e ao aparelho reiniciar.
 *
 * Como os dois passam por aqui, não há dois caminhos para a mesma coisa: um só
 * lugar guarda (a fila), envia (este método) e, no servidor, registra a rota.
 *
 * SEM REDE o envio falha e NADA é marcado como enviado — a fila guarda e a
 * próxima chamada sobe tudo que ficou para trás. É isso que faz o "ao vivo"
 * funcionar offline também: ele não perde ponto, só se atualiza quando o sinal
 * volta, e o painel "pula" para a posição atual nesse momento.
 */
object SincronizadorPosicoes {
    /** O mesmo teto do app ao montar o lote; ver GuiaPosicaoController. */
    private const val TAMANHO_DO_LOTE = 500

    /**
     * Teto por chamada: ~8 mil pontos, mais de um dia de captura. Evita que uma
     * fila gigante (horas sem sinal) prenda a subida até o Android matá-la.
     */
    private const val MAXIMO_DE_LOTES = 16

    /*
     * Uma subida por vez.
     *
     * O serviço dispara a cada ~8s e o WorkManager pode disparar em paralelo.
     * Sem esta trava, duas subidas leriam os MESMOS pendentes e mandariam o
     * mesmo lote duas vezes. O servidor até descartaria pela restrição única,
     * mas seria tráfego à toa e corrida no marcarEnviadas. Com tryLock, a
     * segunda chamada simplesmente desiste: a que já está rodando lê a fila a
     * cada lote e leva junto o que acabou de entrar.
     */
    private val trava = Mutex()

    /**
     * Esvazia a fila local, em lotes.
     *
     * @return true se subiu tudo (ou não havia nada / guia deslogado / outra
     *   subida em andamento); false se faltou subir — tipicamente sem rede.
     */
    suspend fun subir(contexto: Context): Boolean {
        Sessao.carregar(contexto)
        // Sem token não há para quem mandar. Não é falha: é o guia deslogado.
        if (!Sessao.autenticado) return true

        // Já há uma subida em andamento: ela cuida do que está na fila.
        if (!trava.tryLock()) return true

        try {
            val dao = BancoLocal.obter(contexto).posicoes()
            val formato = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)

            repeat(MAXIMO_DE_LOTES) {
                val pendentes = dao.pendentes(TAMANHO_DO_LOTE)
                if (pendentes.isEmpty()) return true

                val lote = LotePosicoes(
                    posicoes = pendentes.map {
                        PosicaoEnviada(
                            latitude = it.latitude,
                            longitude = it.longitude,
                            precisao = it.precisao,
                            velocidade = it.velocidade,
                            capturado_em = formato.format(Date(it.capturadoEm)),
                            mapa_id = it.mapaId,
                        )
                    }
                )

                try {
                    Rede.api.enviarPosicoes(lote)
                    dao.marcarEnviadas(pendentes.map { it.id })
                } catch (erro: Exception) {
                    // Falhou (sem rede, p.ex.): nada marcado, a fila guarda e a
                    // próxima chamada sobe de novo. Ponto nenhum se perde.
                    return false
                }
            }

            return true
        } finally {
            trava.unlock()
        }
    }
}
