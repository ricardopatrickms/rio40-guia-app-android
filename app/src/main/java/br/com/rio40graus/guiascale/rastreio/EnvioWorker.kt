package br.com.rio40graus.guiascale.rastreio

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import br.com.rio40graus.guiascale.dados.BancoLocal
import br.com.rio40graus.guiascale.rede.LotePosicoes
import br.com.rio40graus.guiascale.rede.PosicaoEnviada
import br.com.rio40graus.guiascale.rede.Rede
import br.com.rio40graus.guiascale.rede.Sessao
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Quem esvazia a fila de posições.
 *
 * Roda pelo WorkManager, e não dentro do serviço, por um motivo prático: o
 * WorkManager sobrevive ao app ser fechado e ao aparelho reiniciar, e só
 * acorda quando há rede. O serviço cuida de capturar; subir é problema daqui.
 *
 * Envia em lotes de até 500 e repete enquanto sobrar coisa na fila — depois de
 * horas sem sinal são milhares de pontos, e uma requisição só não passaria.
 */
class EnvioWorker(
    contexto: Context,
    parametros: WorkerParameters,
) : CoroutineWorker(contexto, parametros) {

    override suspend fun doWork(): Result {
        Sessao.carregar(applicationContext)

        // Sem token não há para quem mandar. Não é falha: é o guia deslogado.
        if (!Sessao.autenticado) return Result.success()

        val dao = BancoLocal.obter(applicationContext).posicoes()
        val formato = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)

        repeat(MAXIMO_DE_LOTES_POR_EXECUCAO) {
            val pendentes = dao.pendentes(TAMANHO_DO_LOTE)
            if (pendentes.isEmpty()) return Result.success()

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
                /*
                 * Falhou: nada é marcado como enviado e o WorkManager tenta de
                 * novo, com espera crescente. A fila é a rede de segurança —
                 * ponto nenhum se perde por erro de envio.
                 */
                return Result.retry()
            }
        }

        return Result.success()
    }

    companion object {
        private const val TAMANHO_DO_LOTE = 500

        /**
         * Teto por execução: ~8 mil pontos, mais de um dia de captura. Evita
         * que uma fila gigante prenda o worker até o Android matá-lo.
         */
        private const val MAXIMO_DE_LOTES_POR_EXECUCAO = 16

        private const val NOME = "envio-de-posicoes"

        /**
         * O envio periódico.
         *
         * 15 minutos é o menor intervalo que o Android aceita para trabalho
         * periódico. O serviço também dispara um envio avulso quando a fila
         * cresce, então na prática o atraso é bem menor — isto aqui é a rede
         * de segurança para quando o app não está capturando.
         */
        fun agendar(contexto: Context) {
            val exigencias = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val trabalho = PeriodicWorkRequestBuilder<EnvioWorker>(15, TimeUnit.MINUTES)
                .setConstraints(exigencias)
                .build()

            WorkManager.getInstance(contexto).enqueueUniquePeriodicWork(
                NOME,
                ExistingPeriodicWorkPolicy.KEEP,
                trabalho,
            )
        }
    }
}
