package br.com.rio40graus.guiascale.push

import android.content.Context
import android.util.Log
import br.com.rio40graus.guiascale.BuildConfig
import br.com.rio40graus.guiascale.rede.PedidoDispositivo
import br.com.rio40graus.guiascale.rede.Rede
import br.com.rio40graus.guiascale.rede.Sessao
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Liga este celular ao guia logado na guias-api (tabela guias_dispositivos),
 * para o servidor saber para onde mandar o push.
 *
 * Nada aqui pode travar o app: sem sinal, sem Firebase ou com erro da API,
 * simplesmente não registra — o app reenvia a cada abertura, então a próxima
 * tentativa conserta.
 */
object Dispositivo {

    /** Teto de espera: no logout o guia muitas vezes está sem sinal. */
    private const val ESPERA_MS = 5_000L

    /** Login, abertura do app. */
    suspend fun registrar(contexto: Context) {
        if (!podeRegistrar(contexto)) return
        val token = tokenAtual() ?: return
        enviar(token)
    }

    /** O Firebase trocou o token (PushService.onNewToken). */
    suspend fun registrar(contexto: Context, token: String) {
        if (!podeRegistrar(contexto)) return
        enviar(token)
    }

    /**
     * Logout. Precisa vir ANTES de invalidar a sessão: a rota é autenticada,
     * e depois do logout o servidor já não sabe de quem é o pedido.
     */
    suspend fun remover(contexto: Context) {
        if (!podeRegistrar(contexto)) return
        withTimeoutOrNull(ESPERA_MS) {
            val token = tokenAtual() ?: return@withTimeoutOrNull
            runCatching { Rede.api.removerDispositivo(PedidoDispositivo(token)) }
        }
    }

    // Só guia recebe push; e o homolog não tem Firebase (ver Avisos).
    private fun podeRegistrar(contexto: Context): Boolean =
        Sessao.autenticado && !Sessao.ehAdmin && Avisos.firebaseAtivo(contexto)

    private suspend fun enviar(token: String) {
        withTimeoutOrNull(ESPERA_MS) {
            runCatching {
                Rede.api.registrarDispositivo(
                    PedidoDispositivo(token = token, versao = BuildConfig.VERSION_NAME)
                )
            }.onFailure { Log.w(PushService.TAG, "Falha ao registrar o aparelho", it) }
        }
    }

    // getToken deprecado no firebase-messaging 25.1 — ver PushService.onNewToken.
    @Suppress("DEPRECATION")
    private suspend fun tokenAtual(): String? = withTimeoutOrNull(ESPERA_MS) {
        suspendCancellableCoroutine { continuacao ->
            FirebaseMessaging.getInstance().token.addOnCompleteListener { tarefa ->
                continuacao.resume(if (tarefa.isSuccessful) tarefa.result else null)
            }
        }
    }
}
