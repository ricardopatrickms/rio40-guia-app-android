package br.com.rio40graus.guiascale.rede

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.edit
import br.com.rio40graus.guiascale.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * O logo da agência, o mesmo que o app web mostra.
 *
 * A guias-api não tem esse dado — não há tabela de configuração nem rota para
 * ele. Quem guarda é o Supabase do app web: a linha única de `app_settings`
 * aponta para um PNG no bucket público `guides`, e é o que a tela de
 * Configurações grava quando um administrador troca a imagem. Ler da mesma
 * fonte é o que garante que trocar o logo lá troque aqui também, sem ninguém
 * precisar publicar uma versão nova do app.
 *
 * São duas requisições: uma para descobrir a URL, outra para baixar a imagem.
 * Nenhuma delas exige login, então o logo aparece já na tela de entrada.
 */
object LogoDaAgencia {

    private const val ARQUIVO = "guiascale.marca"
    private const val CHAVE_ORIGEM = "logo_url"
    private const val NOME_CACHE = "logo-agencia.png"

    /*
     * Cliente próprio, e não o de Rede.
     *
     * O cliente de lá carrega um interceptor que anexa o token do guia em toda
     * requisição. Reaproveitá-lo aqui mandaria esse token para o Supabase, que
     * é um servidor de outra empresa e não tem nada com ele. Um token de acesso
     * só deve chegar em quem o emitiu.
     */
    private val cliente = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private fun arquivo(contexto: Context) = File(contexto.filesDir, NOME_CACHE)

    private fun prefs(contexto: Context) =
        contexto.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE)

    /**
     * O que já está no aparelho, sem tocar na rede.
     *
     * É o que a tela desenha primeiro. O guia abre o app em estrada sem sinal
     * com alguma frequência, e nesse caso o logo continua aparecendo em vez de
     * piscar o ícone genérico a cada abertura.
     */
    suspend fun doCache(contexto: Context): ImageBitmap? = withContext(Dispatchers.IO) {
        val arquivo = arquivo(contexto)
        if (!arquivo.exists()) return@withContext null
        runCatching { BitmapFactory.decodeFile(arquivo.path)?.asImageBitmap() }.getOrNull()
    }

    /**
     * Confere no Supabase se o logo mudou e, se mudou, baixa o novo.
     *
     * Devolve a imagem apenas quando houve troca — se o cache já está correto,
     * devolve null e a tela não redesenha à toa. Qualquer falha (sem rede,
     * servidor fora, logo ainda não cadastrado) também devolve null: o logo é
     * enfeite, e nada aqui pode impedir o guia de entrar e ligar o rastreio.
     */
    suspend fun atualizar(contexto: Context): ImageBitmap? = withContext(Dispatchers.IO) {
        runCatching {
            val origem = buscarUrl() ?: return@runCatching null
            val arquivo = arquivo(contexto)

            val jaTemos = prefs(contexto).getString(CHAVE_ORIGEM, null) == origem
            if (jaTemos && arquivo.exists()) return@runCatching null

            val bytes = baixar(origem) ?: return@runCatching null
            arquivo.writeBytes(bytes)
            prefs(contexto).edit { putString(CHAVE_ORIGEM, origem) }

            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()
    }

    /** A linha única de `app_settings`. `logo_url` é nulo até alguém subir um. */
    private fun buscarUrl(): String? {
        val pedido = Request.Builder()
            .url("${BuildConfig.SUPABASE_URL}/rest/v1/app_settings?select=logo_url&limit=1")
            .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .header("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
            .build()

        cliente.newCall(pedido).execute().use { resposta ->
            if (!resposta.isSuccessful) return null
            val corpo = resposta.body?.string().orEmpty()
            val primeira = JSONArray(corpo).optJSONObject(0) ?: return null
            val url = primeira.optString(CHAVE_ORIGEM)
            return url.takeIf { it.isNotBlank() && it != "null" }
        }
    }

    /*
     * O bucket é público, então a imagem vai sem cabeçalho nenhum. O limite de
     * tamanho existe porque quem sobe o arquivo é um administrador pela tela de
     * Configurações, sem validação de dimensão: um PNG de câmera entraria
     * inteiro na memória do aparelho do guia.
     */
    private fun baixar(url: String): ByteArray? {
        cliente.newCall(Request.Builder().url(url).build()).execute().use { resposta ->
            if (!resposta.isSuccessful) return null
            val corpo = resposta.body ?: return null
            if (corpo.contentLength() > 2 * 1024 * 1024) return null
            return corpo.bytes()
        }
    }
}
