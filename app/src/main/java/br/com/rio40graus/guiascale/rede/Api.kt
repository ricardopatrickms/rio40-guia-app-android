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
    val status: StatusReserva?,
    val latitude: Double?,
    val longitude: Double?,
)

data class StatusReserva(
    val id: Int,
    val nome: String?,
    val cor: String?,
)

/**
 * Troca de status da reserva no embarque.
 *
 * O guia só pode marcar três: 2 CHECK-IN, 3 NO-SHOW e 8 PARCIAL. Os dois
 * últimos exigem motivo e mexem em fatura — por isso o app manda apenas o
 * check-in, e o resto segue pelo app web, onde a tela para escolher motivo
 * existe. Ver GuiaReservaStatusController.
 */
data class PedidoStatus(val status_id: Int)

const val STATUS_CHECK_IN = 2

interface ApiGuias {

    @POST("guia/login")
    suspend fun login(@Body credenciais: PedidoLogin): RespostaLogin

    @POST("guia/posicoes")
    suspend fun enviarPosicoes(@Body lote: LotePosicoes): RespostaLote

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

    @POST("guia/reserva/{reservaId}/status")
    suspend fun trocarStatus(
        @Path("reservaId") reservaId: Int,
        @Body corpo: PedidoStatus,
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
