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
import retrofit2.http.GET
import retrofit2.http.POST
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

data class RespostaLogin(
    val access_token: String?,
    val token_type: String?,
    val expires_in: Int?,
)

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
 * A resposta traz muito mais do que isto — veículo, motorista, fornecedores,
 * totais financeiros, ocupação. Aqui só entra o que a tela de embarque usa: o
 * app do guia não é o painel, e cada campo declarado é um campo a manter.
 */
data class RespostaMapas(
    val data: String?,
    val mapas: List<MapaEmbarque> = emptyList(),
)

data class MapaEmbarque(
    val id: Int,
    val tour: String?,
    val bloqueado: Boolean = false,
    val reservas: List<ReservaEmbarque> = emptyList(),
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
    val adt: Int = 0,
    val chd: Int = 0,
    val inf: Int = 0,
    val jovem: Int = 0,
    val idoso: Int = 0,
    val total: Int? = null,
    val parcial: ParcialEmbarque? = null,
    val a_receber: Double = 0.0,
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
)

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
}

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
