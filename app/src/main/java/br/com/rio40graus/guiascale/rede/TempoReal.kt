package br.com.rio40graus.guiascale.rede

import android.os.Handler
import android.os.Looper
import android.util.Log
import br.com.rio40graus.guiascale.BuildConfig
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Tempo real do Geocheck-in — o mesmo Reverb do web (useRealtimeMapa.ts).
 *
 * Fala o protocolo do Pusher direto sobre o WebSocket do OkHttp, sem biblioteca
 * nova: conecta, recebe o socket_id, pede a assinatura de cada canal privado à
 * guias-api (/broadcasting/auth para o admin, /guia/broadcasting/auth para o
 * guia — as mesmas rotas do web) e assina.
 *
 * Como no web, o evento é só um ping: quem escuta rebusca pela API de sempre.
 * Os eventos chegam na thread principal.
 *
 * @param canais nomes sem o prefixo "private-" (ex.: "mapa.12", "guia.34", "guias.todos").
 * @param aoEvento (canal, evento sem o ponto inicial, dados em JSON).
 */
class TempoReal(
    private val canais: List<String>,
    private val aoEvento: (canal: String, evento: String, dados: JsonObject?) -> Unit,
) {
    private val principal = Handler(Looper.getMainLooper())
    private var socket: WebSocket? = null
    private var ativo = false
    private var tentativas = 0

    fun ligar() {
        if (ativo || canais.isEmpty() || BuildConfig.REVERB_KEY.isBlank()) return
        ativo = true
        conectar()
    }

    fun desligar() {
        ativo = false
        principal.removeCallbacksAndMessages(null)
        socket?.close(1000, null)
        socket = null
    }

    private fun conectar() {
        if (!ativo) return
        val esquema = if (BuildConfig.REVERB_TLS) "wss" else "ws"
        val url = "$esquema://${BuildConfig.REVERB_HOST}:${BuildConfig.REVERB_PORT}" +
            "/app/${BuildConfig.REVERB_KEY}?protocol=7&client=android&version=1.0&flash=false"
        socket = cliente.newWebSocket(Request.Builder().url(url).build(), ouvinte)
    }

    private fun reconectarDepois() {
        if (!ativo) return
        socket = null
        // 2s, 4s, 8s… até 30s — o Reverb caído não vira um loop apertado.
        val espera = (2_000L shl tentativas.coerceAtMost(4)).coerceAtMost(30_000L)
        tentativas++
        principal.postDelayed({ conectar() }, espera)
    }

    private val ouvinte = object : WebSocketListener() {
        override fun onMessage(webSocket: WebSocket, text: String) {
            val msg = runCatching { JsonParser.parseString(text).asJsonObject }.getOrNull() ?: return
            val evento = msg.get("event")?.asString ?: return
            when (evento) {
                "pusher:connection_established" -> {
                    Log.i(TAG, "conectado; assinando ${canais.joinToString()}")
                    tentativas = 0
                    val socketId = dadosDe(msg)?.get("socket_id")?.asString ?: return
                    canais.forEach { assinar(webSocket, socketId, it) }
                }
                "pusher:ping" -> webSocket.send("""{"event":"pusher:pong","data":{}}""")
                "pusher_internal:subscription_succeeded" -> Log.i(TAG, "assinado: ${msg.get("channel")?.asString}")
                else -> {
                    if (evento.startsWith("pusher")) return
                    Log.i(TAG, "evento $evento em ${msg.get("channel")?.asString}")
                    val canal = msg.get("channel")?.asString?.removePrefix("private-") ?: return
                    val nome = evento.removePrefix(".")
                    val dados = dadosDe(msg)
                    principal.post { if (ativo) aoEvento(canal, nome, dados) }
                }
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.w(TAG, "Reverb caiu: ${t.message}")
            principal.post { reconectarDepois() }
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            principal.post { reconectarDepois() }
        }
    }

    /** Pede a assinatura do canal privado à guias-api e assina no socket. */
    private fun assinar(webSocket: WebSocket, socketId: String, canal: String) {
        val nomeCompleto = "private-$canal"
        val raiz = BuildConfig.API_URL.trimEnd('/').removeSuffix("/api")
        val rota = if (Sessao.ehAdmin) "$raiz/broadcasting/auth" else "$raiz/guia/broadcasting/auth"
        val pedido = Request.Builder()
            .url(rota)
            .header("Accept", "application/json")
            .apply { Sessao.token?.let { header("Authorization", "Bearer $it") } }
            .post(
                FormBody.Builder()
                    .add("socket_id", socketId)
                    .add("channel_name", nomeCompleto)
                    .build(),
            )
            .build()
        cliente.newCall(pedido).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.w(TAG, "auth $canal falhou: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val corpo = it.body?.string().orEmpty()
                    val auth = runCatching {
                        JsonParser.parseString(corpo).asJsonObject.get("auth").asString
                    }.getOrNull()
                    if (!it.isSuccessful || auth == null) {
                        Log.w(TAG, "auth $canal negado (${it.code})")
                        return
                    }
                    val assinatura = JsonObject().apply {
                        addProperty("event", "pusher:subscribe")
                        add("data", JsonObject().apply {
                            addProperty("auth", auth)
                            addProperty("channel", nomeCompleto)
                        })
                    }
                    webSocket.send(assinatura.toString())
                }
            }
        })
    }

    /** O "data" do Pusher vem como string JSON (às vezes já como objeto). */
    private fun dadosDe(msg: JsonObject): JsonObject? {
        val d = msg.get("data") ?: return null
        return runCatching {
            if (d.isJsonPrimitive) JsonParser.parseString(d.asString).asJsonObject else d.asJsonObject
        }.getOrNull()
    }

    private companion object {
        const val TAG = "TempoReal"
        val cliente: OkHttpClient = OkHttpClient.Builder()
            .pingInterval(30, TimeUnit.SECONDS)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()
    }
}
