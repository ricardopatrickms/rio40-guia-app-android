package br.com.rio40graus.guiascale.rede

import android.content.Context
import br.com.rio40graus.guiascale.BuildConfig
import br.com.rio40graus.guiascale.R
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * Credenciais do guia — as mesmas do app web.
 *
 * Os nomes seguem o que a guias-api valida: `login` e `senha`, não `email`
 * e `password`. Ver GuiaAuthController.
 */
data class PedidoLogin(val login: String, val senha: String)

/**
 * Login e `GET guia/me` devolvem o mesmo envelope de perfil.
 * O nome do guia vem em `guia.nome` / `user.user_metadata.nome` — igual ao web.
 */
data class RespostaLogin(
    val access_token: String?,
    val token_type: String?,
    val expires_in: Int?,
    val user: UsuarioLogin? = null,
    val guia: GuiaPerfil? = null,
)

data class RespostaMe(
    val user: UsuarioLogin? = null,
    val guia: GuiaPerfil? = null,
)

data class UsuarioLogin(
    val id: String? = null,
    val email: String? = null,
    val user_metadata: UsuarioMeta? = null,
)

data class UsuarioMeta(
    val nome: String? = null,
    val foto: String? = null,
)

data class GuiaPerfil(
    val id: String? = null,
    val nome: String? = null,
    val status: String? = null,
    val parceiro_id: Int? = null,
)

/** Nome de exibição: o mesmo critério do web (pessoa.pes_nome). */
fun nomeDoGuia(user: UsuarioLogin?, guia: GuiaPerfil?): String? =
    guia?.nome?.takeIf { it.isNotBlank() }
        ?: user?.user_metadata?.nome?.takeIf { it.isNotBlank() }

/** `usuarios.usu_id` — login e /me devolvem como string em user.id / guia.id. */
fun usuIdDoGuia(user: UsuarioLogin?, guia: GuiaPerfil?): Int? =
    user?.id?.toIntOrNull()
        ?: guia?.id?.toIntOrNull()


/**
 * Uma leitura, no formato que a guias-api espera.
 *
 * `capturado_em` vai em ISO 8601 COM fuso. O servidor guarda no fuso da
 * aplicação, e sem o deslocamento um ponto capturado às 9h viraria 6h ou 12h
 * dependendo de onde o aparelho estiver.
 */
data class PosicaoEnviada(
    val latitude: Double,
    val longitude: Double,
    val precisao: Float?,
    val velocidade: Float?,
    val capturado_em: String,
    val mapa_id: Int?,
)

data class LotePosicoes(val posicoes: List<PosicaoEnviada>)

data class RespostaLote(val ok: Boolean, val gravadas: Int)

/** Trajeto já no servidor — um dia, opcionalmente de um mapa só. */
data class RespostaPosicoesDia(
    val data: String?,
    val total: Int? = 0,
    val posicoes: List<PosicaoRemota> = emptyList(),
)

data class PosicaoRemota(
    val latitude: Double,
    val longitude: Double,
    val precisao: Float? = null,
    val velocidade: Float? = null,
    val capturado_em: String? = null,
    val recebido_em: String? = null,
    val mapa_id: Int? = null,
)


/**
 * O mapa de embarque do dia, como a guias-api devolve em `guia/mapa-embarque`.
 *
 * Campos extras (ocupação, totais, fornecedores…) alimentam a aba Embarque —
 * o Geocheck-in continua usando só o que já usava.
 */
data class RespostaMapas(
    val data: String?,
    val mapas: List<MapaEmbarque> = emptyList(),
)

data class MapaEmbarque(
    val id: Int,
    val tour: String?,
    val tour_id: Int? = null,
    val eh_apoio: Boolean = false,
    val bloqueado: Boolean = false,
    val veiculo: VeiculoMapa? = null,
    val motorista: MotoristaMapa? = null,
    val fornecedor: String? = null,
    val transporte_despesa: TransporteDespesa? = null,
    val fornecedores: List<FornecedorMapa> = emptyList(),
    val ocupacao: OcupacaoMapa? = null,
    val totais: TotaisMapa? = null,
    val caixa: CaixaMapa? = null,
    val reservas: List<ReservaEmbarque> = emptyList(),
)

data class VeiculoMapa(
    val tipo: String? = null,
    val modelo: String? = null,
    val capacidade: Int? = null,
    val placa: String? = null,
)

data class MotoristaMapa(
    val nome: String? = null,
    val telefone: String? = null,
)

data class TransporteDespesa(
    val acerto_fornecedor_id: Int = 0,
    val valor: Double = 0.0,
    val descricao: String? = null,
    val permite_pagamento: Boolean = false,
    val pagamentos: List<PagamentoFornecedor> = emptyList(),
)

data class FornecedorMapa(
    val origem: String? = null,
    val permite_pagamento: Boolean = false,
    val acerto_fornecedor_id: Int? = null,
    val fornecedor_id: Int? = null,
    val nome: String? = null,
    val atividade: String? = null,
    val descricao: String? = null,
    val valor: Double = 0.0,
    val pax: Int? = null,
    val pagamentos: List<PagamentoFornecedor> = emptyList(),
)

data class PagamentoFornecedor(
    val id: Int = 0,
    val forma_id: Int? = null,
    val forma: String? = null,
    val parcela_id: Int? = null,
    val parcela: String? = null,
    val valor: Double = 0.0,
)

data class OcupacaoMapa(
    val pax: Int = 0,
    val capacidade: Int? = null,
    val percentual: Int? = null,
)

data class TotaisMapa(
    val reservas: Int = 0,
    val adt: Int = 0,
    val chd: Int = 0,
    val inf: Int = 0,
    val jovem: Int = 0,
    val idoso: Int = 0,
    val pax: Int = 0,
    val valor: Double = 0.0,
    val a_receber: Double = 0.0,
    val a_pagar: Double = 0.0,
)

data class CaixaMapa(
    val recebido: Double = 0.0,
    val adiantamentos: Double = 0.0,
    val pago: Double = 0.0,
    val saldo: Double = 0.0,
)

data class ReservaEmbarque(
    val id: Int,
    val hora: String?,
    val embarque: String?,
    val endereco: String?,
    val bairro: String?,
    val apto: String?,
    val pax: String?,
    val voucher: String?,
    val passeio: String? = null,
    val idioma: String? = null,
    val idioma_id: Int? = null,
    val idioma_nome: String? = null,
    val adt: Int = 0,
    val chd: Int = 0,
    val inf: Int = 0,
    val jovem: Int = 0,
    val idoso: Int = 0,
    val total: Int? = null,
    val parcial: ParcialEmbarque? = null,
    val a_receber: Double = 0.0,
    val valor: Double = 0.0,
    val taxas: Double = 0.0,
    val cortesia: Double? = null,
    val telefone: String? = null,
    val observacao: String? = null,
    val checked_in_at: String? = null,
    val pagamentos: List<PagamentoLancado> = emptyList(),
    val status: StatusReserva?,
    val latitude: Double?,
    val longitude: Double?,
)

data class ParcialEmbarque(
    val adulto: Int = 0,
    val chd: Int = 0,
    val infantil: Int = 0,
    val jovem: Int = 0,
    val idoso: Int = 0,
)

data class PagamentoLancado(
    val pag_id: Int = 0,
    val forma_id: Int = 0,
    val forma: String? = null,
    val tipo: String? = null,
    val parcela_id: Int? = null,
    val parcela: String? = null,
    val valor: Double = 0.0,
    val taxa: Double = 0.0,
)

data class OcorrenciaMapa(
    val id: Int,
    val relato: String?,
    val status: String? = null,
    val usuario_id: Int? = null,
    val autor_nome: String? = null,
    val data_adicionado: String? = null,
    val data_alterado: String? = null,
)

data class RespostaOcorrencias(
    val ocorrencias: List<OcorrenciaMapa> = emptyList(),
)

data class PedidoOcorrencia(val relato: String)

data class FormaPagamento(
    val id: Int,
    val nome: String?,
    val tipo: String?,
    val credito: Boolean = false,
)

data class ParcelaOpcao(
    val id: Int,
    val nome: String?,
    val taxa: Double = 0.0,
)

data class IdiomaOpcao(
    val id: Int,
    val nome: String?,
    val codigo: String?,
)

data class StatusReserva(
    val id: Int,
    val nome: String?,
    val cor: String?,
)

/**
 * Troca de status da reserva no embarque.
 *
 * O guia só pode marcar três: 2 CHECK-IN, 3 NO-SHOW e 8 PARCIAL.
 * NO-SHOW exige motivo_id; PARCIAL manda quantos NÃO embarcaram por categoria.
 * Ver GuiaReservaStatusController.
 */
data class PedidoStatus(
    val status_id: Int,
    val motivo_id: Int? = null,
    val adulto: Int? = null,
    val chd: Int? = null,
    val infantil: Int? = null,
    val jovem: Int? = null,
    val idoso: Int? = null,
)

data class PedidoIdioma(val idioma_id: Int)

data class PedidoPagamentos(val pagamentos: List<ItemPagamento>)

data class ItemPagamento(
    val forma_id: Int,
    val valor: Double,
    val parcela_id: Int? = null,
    val pag_id: Int? = null,
)

/** Lançamento de pagamento AO FORNECEDOR (acerto). */
data class PedidoPagamentosFornecedor(val pagamentos: List<ItemPagamentoFornecedor>)

data class ItemPagamentoFornecedor(
    val forma_id: Int,
    val valor: Double,
    val parcela_id: Int? = null,
    val id: Int? = null,
)

data class CategoriaMotivo(val id: Int, val nome: String?)

data class MotivoStatus(
    val mot_id: Int,
    val mot_nome: String?,
    val categoria_id: Int?,
)

data class RespostaMotivos(
    val categorias: List<CategoriaMotivo> = emptyList(),
    val motivos: List<MotivoStatus> = emptyList(),
)

data class RespostaFormas(val formas: List<FormaPagamento> = emptyList())
data class RespostaParcelas(val parcelas: List<ParcelaOpcao> = emptyList())
data class RespostaIdiomas(val idiomas: List<IdiomaOpcao> = emptyList())

const val STATUS_CHECK_IN = 2
const val STATUS_NO_SHOW = 3
const val STATUS_PARCIAL = 8
const val STATUS_RESERVADO = 1

interface ApiGuias {

    @POST("guia/login")
    suspend fun login(@Body credenciais: PedidoLogin): RespostaLogin

    @GET("guia/me")
    suspend fun me(): RespostaMe

    @POST("guia/posicoes")
    suspend fun enviarPosicoes(@Body lote: LotePosicoes): RespostaLote

    /** Trajeto gravado no servidor; com mapa_id vem só o daquele embarque. */
    @GET("guia/posicoes")
    suspend fun listarPosicoes(
        @Query("data") data: String? = null,
        @Query("mapa_id") mapaId: Int? = null,
    ): RespostaPosicoesDia

    /*
     * Invalida o token no servidor. Devolve Response, e não o corpo, porque
     * quem sai do app não pode depender da resposta: o guia desliga no fim do
     * passeio, muitas vezes sem sinal, e a saída local acontece de todo jeito.
     */
    @POST("guia/logout")
    suspend fun logout(): Response<Unit>

    /** Mapas do dia, com as reservas e as coordenadas de cada ponto. */
    @GET("guia/mapa-embarque")
    suspend fun mapaEmbarque(@Query("data") data: String? = null): RespostaMapas

    @GET("guia/reserva/status-motivos")
    suspend fun statusMotivos(): RespostaMotivos

    @GET("guia/formas-pagamento")
    suspend fun formasPagamento(): RespostaFormas

    @GET("guia/parcelas")
    suspend fun parcelas(): RespostaParcelas

    @GET("guia/idiomas")
    suspend fun idiomas(): RespostaIdiomas

    @POST("guia/reserva/{reservaId}/status")
    suspend fun trocarStatus(
        @Path("reservaId") reservaId: Int,
        @Body corpo: PedidoStatus,
    ): Response<Unit>

    @POST("guia/reserva/{reservaId}/pagamentos")
    suspend fun salvarPagamentos(
        @Path("reservaId") reservaId: Int,
        @Body corpo: PedidoPagamentos,
    ): Response<Unit>

    @POST("guia/reserva/{reservaId}/idioma")
    suspend fun salvarIdioma(
        @Path("reservaId") reservaId: Int,
        @Body corpo: PedidoIdioma,
    ): Response<Unit>

    @POST("guia/fornecedor/{acertoFornecedorId}/pagamentos")
    suspend fun salvarPagamentosFornecedor(
        @Path("acertoFornecedorId") acertoFornecedorId: Int,
        @Body corpo: PedidoPagamentosFornecedor,
    ): Response<Unit>

    @GET("guia/mapa/{mapaId}/ocorrencias")
    suspend fun listarOcorrencias(@Path("mapaId") mapaId: Int): RespostaOcorrencias

    @POST("guia/mapa/{mapaId}/ocorrencias")
    suspend fun criarOcorrencia(
        @Path("mapaId") mapaId: Int,
        @Body corpo: PedidoOcorrencia,
    ): Response<Unit>

    @PUT("guia/ocorrencia/{id}")
    suspend fun editarOcorrencia(
        @Path("id") id: Int,
        @Body corpo: PedidoOcorrencia,
    ): Response<Unit>

    @DELETE("guia/ocorrencia/{id}")
    suspend fun excluirOcorrencia(@Path("id") id: Int): Response<Unit>

    @GET("guia/checklist")
    suspend fun checklist(
        @Query("tour_id") tourId: Int,
        @Query("data") data: String,
        @Query("mapa_id") mapaId: Int? = null,
    ): RespostaChecklist

    @POST("guia/checklist/{tarefaId}/feito")
    suspend fun marcarChecklist(
        @Path("tarefaId") tarefaId: String,
        @Body corpo: PedidoChecklistFeito,
    ): Response<Unit>

    @GET("guia/painel")
    suspend fun painel(@Query("periodo") periodo: String): KpisDoPainel

    @GET("guia/passeios-guia")
    suspend fun passeiosGuia(@Query("periodo") periodo: String): PasseiosDoGuia

    @GET("guia/agenda")
    suspend fun agenda(
        @Query("de") de: String,
        @Query("ate") ate: String,
    ): AgendaDoGuia

    @GET("guia/ficha")
    suspend fun ficha(): FichaGuia

    @GET("guia/lei-progresso")
    suspend fun leiProgresso(): ProgressoDaLei

    @POST("guia/lei-progresso")
    suspend fun salvarLeiProgresso(@Body corpo: ProgressoDaLei): Response<Unit>

    @GET("guia/disponibilidade")
    suspend fun listarBloqueios(@Query("from") from: String? = null): RespostaBloqueios

    @POST("guia/disponibilidade")
    suspend fun criarBloqueios(@Body corpo: PedidoBloqueios): RespostaBloqueios

    @POST("guia/disponibilidade/remover")
    suspend fun removerBloqueios(@Body corpo: PedidoRemoverBloqueios): Response<Unit>

    @GET("guia/disponibilidade/dias-semana")
    suspend fun diasSemana(): DiasSemanaGuia

    @GET("guia/disponibilidade/dias-ocupados")
    suspend fun diasOcupados(@Query("from") from: String? = null): RespostaDiasOcupados

    @GET("guia/solicitacoes-trabalho")
    suspend fun listarSolicitacoes(@Query("from") from: String? = null): RespostaSolicitacoes

    @POST("guia/solicitacoes-trabalho")
    suspend fun criarSolicitacoes(@Body corpo: PedidoSolicitacoes): RespostaSolicitacoes
}

data class RespostaChecklist(
    val tarefas: List<TarefaChecklist> = emptyList(),
)

data class TarefaChecklist(
    val id: String,
    val title: String?,
    val order_index: Int = 0,
    val tours: List<Int> = emptyList(),
    val done: Boolean = false,
    val legado: Boolean = false,
)

data class PedidoChecklistFeito(
    val mapa_id: Int,
    val data: String,
    val done: Boolean,
)

data class KpisDoPainel(
    val periodo: PeriodoPainel? = null,
    val diasTrabalhados: Int = 0,
    val passeiosRealizados: Int = 0,
    val passeiosLista: List<PasseioListaItem> = emptyList(),
    val totalServicos: Int = 0,
    val passeiosDistintos: Int = 0,
    val agendamentos: Int = 0,
    val ultimos7dias: List<ItemAgenda> = emptyList(),
    val proximos: List<ItemAgenda> = emptyList(),
    val receita: Double = 0.0,
    val evolucao: List<EvolucaoMes> = emptyList(),
    val transportes: List<TransporteRanking> = emptyList(),
    val passeiosRanking: List<PasseioRanking> = emptyList(),
    val aniversario: String? = null,
)

data class PeriodoPainel(val de: String? = null, val ate: String? = null)

data class PasseioListaItem(
    val id: String? = null,
    val date: String? = null,
    val tour_name: String? = null,
    val start_time: String? = null,
    val transport: String? = null,
    val company: String? = null,
    val status: String? = null,
)

data class ItemAgenda(
    val id: String? = null,
    val date: String? = null,
    val tour_name: String? = null,
    val start_time: String? = null,
    val transport: String? = null,
    val company: String? = null,
    val pax: Int = 0,
    val status: String? = null,
)

data class EvolucaoMes(
    val mes: String? = null,
    val diasTrabalhados: Int = 0,
    val passeios: Int = 0,
    val receita: Double = 0.0,
)

data class TransporteRanking(
    val nome: String? = null,
    val quantidade: Int = 0,
    val percentual: Double = 0.0,
)

data class PasseioRanking(
    val nome: String? = null,
    val qty: Int = 0,
)

data class PasseiosDoGuia(
    val periodo: PeriodoPainel? = null,
    val comparativo: List<ComparativoPasseio> = emptyList(),
    val detalhamento: List<DetalhePasseio> = emptyList(),
)

data class ComparativoPasseio(
    val nome: String? = null,
    val atual: Int = 0,
    val anterior: Int = 0,
)

data class DetalhePasseio(
    val nome: String? = null,
    val qty: Int = 0,
    val pax: Int = 0,
    val receita: Double = 0.0,
    val avaliacao: Double? = null,
)

data class AgendaDoGuia(
    val periodo: PeriodoPainel? = null,
    val itens: List<ItemAgenda> = emptyList(),
)

data class FichaGuia(
    val guide: GuiaFichaDados? = null,
    val estatisticas: GuiaFichaStats? = null,
    val especialidades: List<EspecialidadeGuia> = emptyList(),
)

data class GuiaFichaDados(
    val usu_id: Int? = null,
    val name: String? = null,
    val photo_url: String? = null,
    val cadastur: String? = null,
    val whatsapp: String? = null,
    val email: String? = null,
    val data_entrada: String? = null,
    val level: String? = null,
    val status: String? = null,
    val observacoes: String? = null,
)

data class GuiaFichaStats(
    val anosEmpresa: Int = 0,
    val tempoEmpresa: String? = null,
    val totalPasseios: Int = 0,
    val nivel: String? = null,
    val ocorrenciasAno: Int = 0,
    val avaliacaoMedia: Double? = null,
    val pontualidadePct: Double? = null,
)

data class EspecialidadeGuia(
    val destino: String? = null,
    val nivel: Int = 0,
)

data class ProgressoDaLei(
    val open_ids: List<String> = emptyList(),
    val last_section: String? = null,
)

data class RespostaBloqueios(
    val bloqueios: List<BloqueioGuia> = emptyList(),
)

data class BloqueioGuia(
    val id: String? = null,
    val guide_id: String? = null,
    val usu_id: Int = 0,
    val date: String,
    val status: String? = null,
    val reason: String? = null,
    val source: String? = null,
)

data class PedidoBloqueios(
    val dates: List<String>,
    val reason: String? = null,
    val source: String = "app-guia",
)

data class PedidoRemoverBloqueios(
    val dates: List<String>,
)

data class DiasSemanaGuia(
    val dias: List<Int> = emptyList(),
    val restrito: Boolean = false,
)

data class RespostaDiasOcupados(
    val dias: List<String> = emptyList(),
)

data class RespostaSolicitacoes(
    val solicitacoes: List<SolicitacaoTrabalho> = emptyList(),
)

data class SolicitacaoTrabalho(
    val id: String? = null,
    val usu_id: Int = 0,
    val date: String,
    val reason: String? = null,
    val source: String? = null,
    val situacao: String? = null,
    val motivo_decisao: String? = null,
)

data class PedidoSolicitacoes(
    val dates: List<String>,
    val reason: String? = null,
    val source: String = "app-guia",
)

/**
 * O cliente HTTP do app.
 *
 * O token entra por interceptor, e não em cada chamada: assim nenhuma
 * requisição nova esquece de mandá-lo. Quem guarda o token é a Sessao.
 *
 * Os tempos são curtos de propósito. O envio roda em segundo plano, e uma
 * requisição pendurada em rede ruim segura a fila inteira — melhor falhar
 * rápido e tentar de novo, que é o que o WorkManager faz.
 */
object Rede {

    private val autenticacao = okhttp3.Interceptor { cadeia ->
        val token = Sessao.token
        val pedido = if (token.isNullOrBlank()) {
            cadeia.request()
        } else {
            cadeia.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/json")
                .build()
        }
        cadeia.proceed(pedido)
    }

    private val cliente = OkHttpClient.Builder()
        .addInterceptor(autenticacao)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BASIC
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            }
        )
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val api: ApiGuias = Retrofit.Builder()
        .baseUrl(BuildConfig.API_URL)
        .client(cliente)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiGuias::class.java)
}

/**
 * A mensagem que o guia lê quando algo falha.
 *
 * Sem isto a tela mostrava "HTTP 401 Unauthorized": o Retrofit lança
 * HttpException, e o `message` dela é o texto técnico do protocolo, não o que
 * o servidor escreveu. A guias-api responde `{"error": "..."}` em português e
 * já distingue senha errada de acesso bloqueado — é essa frase que interessa,
 * e ela estava sendo descartada.
 *
 * Espelha o `translateAuthError` do app web, para os dois dizerem a mesma coisa
 * diante da mesma falha.
 */
fun mensagemDeErro(contexto: Context, erro: Throwable): String = when (erro) {

    is HttpException -> {
        val doServidor = runCatching {
            val corpo = erro.response()?.errorBody()?.string().orEmpty()
            JSONObject(corpo).optString("error").takeIf { it.isNotBlank() }
        }.getOrNull()

        doServidor ?: when (erro.code()) {
            429 -> contexto.getString(R.string.erro_muitas_tentativas)
            else -> contexto.getString(R.string.erro_login)
        }
    }

    // Sem rede, DNS fora, servidor local desligado: tudo chega como IOException.
    is java.io.IOException -> contexto.getString(R.string.erro_sem_conexao)

    else -> contexto.getString(R.string.erro_login)
}

/**
 * A mesma tradução, para quem devolve `Response` em vez de lançar.
 *
 * O Retrofit só lança HttpException quando o método declara o corpo direto.
 * Nas chamadas que declaram `Response<T>` — porque precisam distinguir o
 * código — o erro chega como uma resposta comum, e a frase do servidor está no
 * errorBody.
 */
fun mensagemDeErro(contexto: Context, resposta: Response<*>): String {
    val doServidor = runCatching {
        val corpo = resposta.errorBody()?.string().orEmpty()
        JSONObject(corpo).optString("error").takeIf { it.isNotBlank() }
    }.getOrNull()

    return doServidor ?: when (resposta.code()) {
        429 -> contexto.getString(R.string.erro_muitas_tentativas)
        else -> contexto.getString(R.string.erro_generico)
    }
}
