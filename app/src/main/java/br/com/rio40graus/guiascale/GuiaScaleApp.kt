package br.com.rio40graus.guiascale

import android.app.Application
import android.util.Log
import br.com.rio40graus.guiascale.push.Avisos
import br.com.rio40graus.guiascale.push.PushService
import br.com.rio40graus.guiascale.rastreio.EnvioWorker
import br.com.rio40graus.guiascale.rede.Sessao
import com.google.firebase.messaging.FirebaseMessaging

/**
 * Ponto de entrada do processo.
 *
 * Carrega o token e reagenda o envio a cada abertura — inclusive quando quem
 * acorda o processo é o próprio serviço, depois de o sistema tê-lo matado.
 */
class GuiaScaleApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Sessao.carregar(this)
        EnvioWorker.agendar(this)
        Avisos.criarCanal(this)
        mostrarTokenNoLog()
    }

    /**
     * Até o passo 8 (token enviado à guias-api), o jeito de testar pelo
     * Console do Firebase é copiar o token do Logcat (filtro "Push").
     * Só em debug, e só onde o Firebase subiu — o homolog não tem.
     * (token deprecado no 25.1 — ver PushService.onNewToken.)
     */
    @Suppress("DEPRECATION")
    private fun mostrarTokenNoLog() {
        if (!BuildConfig.DEBUG || !Avisos.firebaseAtivo(this)) return
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token -> Log.d(PushService.TAG, "Token FCM: $token") }
    }
}
