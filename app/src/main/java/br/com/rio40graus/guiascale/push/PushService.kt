package br.com.rio40graus.guiascale.push

import android.util.Log
import br.com.rio40graus.guiascale.BuildConfig
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Porta de entrada do push (FCM).
 *
 * Com o app FECHADO ou em segundo plano, uma mensagem com bloco
 * "notification" é desenhada pelo próprio sistema, no canal e com o ícone do
 * manifesto — este método nem é chamado. Ele recebe as mensagens com o app
 * ABERTO, e as só-de-dados em qualquer estado; nos dois casos quem mostra o
 * aviso somos nós.
 */
class PushService : FirebaseMessagingService() {

    override fun onMessageReceived(mensagem: RemoteMessage) {
        val titulo = mensagem.notification?.title ?: mensagem.data["titulo"]
        val texto = mensagem.notification?.body ?: mensagem.data["mensagem"]
        if (titulo.isNullOrBlank() && texto.isNullOrBlank()) return

        Avisos.mostrar(this, titulo.orEmpty(), texto.orEmpty(), mensagem.data)
    }

    /**
     * O Firebase troca o token do aparelho de tempos em tempos (reinstalação,
     * limpeza de dados): avisa a guias-api, se houver guia logado. Se o envio
     * falhar, a próxima abertura do app reenvia.
     *
     * Deprecado no firebase-messaging 25.1 em favor de register()/onRegistered,
     * que entregam um id de instalação em vez do token. O envio da guias-api
     * (HTTP v1) é por token, então seguimos neste enquanto ele existir.
     */
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onNewToken(token: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, "Token FCM: $token")
        CoroutineScope(Dispatchers.IO).launch {
            Dispositivo.registrar(applicationContext, token)
        }
    }

    companion object {
        const val TAG = "Push"
    }
}
