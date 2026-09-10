package br.com.rio40graus.guiascale

import android.app.Application
import br.com.rio40graus.guiascale.rastreio.EnvioWorker
import br.com.rio40graus.guiascale.rede.Sessao

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
    }
}
