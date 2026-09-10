package br.com.rio40graus.guiascale.rastreio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import br.com.rio40graus.guiascale.MainActivity
import br.com.rio40graus.guiascale.R
import br.com.rio40graus.guiascale.dados.BancoLocal
import br.com.rio40graus.guiascale.dados.Posicao
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch

/**
 * O serviço que mantém a captura viva com o app minimizado.
 *
 * É a razão de o app existir. No navegador o rastreio morre quando a aba sai
 * de foco ou a tela apaga; aqui um serviço em primeiro plano segura o processo
 * e o Android continua entregando posição.
 *
 * "Primeiro plano" é o nome do mecanismo, não do que o guia vê: o app fica
 * minimizado e o que aparece é a notificação permanente. Ela é obrigatória, e
 * é assim que o Android garante que ninguém seja rastreado às escondidas.
 *
 * O serviço só captura e guarda. Enviar é do EnvioWorker — separados porque
 * rede e GPS falham por motivos diferentes, e misturar os dois faria uma falha
 * de rede interromper a captura.
 */
class RastreioService : LifecycleService() {

    private val cliente by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private val dao by lazy { BancoLocal.obter(this).posicoes() }

    private var capturadas = 0

    private val recebedor = object : LocationCallback() {
        override fun onLocationResult(resultado: LocationResult) {
            val posicao = resultado.lastLocation ?: return

            lifecycleScope.launch {
                dao.inserir(
                    Posicao(
                        latitude = posicao.latitude,
                        longitude = posicao.longitude,
                        precisao = if (posicao.hasAccuracy()) posicao.accuracy else null,
                        velocidade = if (posicao.hasSpeed()) posicao.speed else null,
                        capturadoEm = posicao.time,
                        // Lido a cada ponto, e não uma vez só: o guia pode
                        // fazer o check-in DEPOIS de ligar o rastreio, e daí
                        // em diante o percurso passa a ter dono.
                        mapaId = EstadoRastreio.mapaId(this@RastreioService),
                    )
                )

                capturadas++
                atualizarNotificacao()

                /*
                 * Empurra a fila de tempos em tempos, sem esperar o ciclo de 15
                 * minutos do WorkManager. Com rede boa o rastro fica quase ao
                 * vivo; sem rede, o pedido falha e a fila continua guardando.
                 */
                if (capturadas % PONTOS_ENTRE_ENVIOS == 0) {
                    WorkManager.getInstance(applicationContext)
                        .enqueue(OneTimeWorkRequestBuilder<EnvioWorker>().build())
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        criarCanal()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (intent?.action == ACAO_PARAR) {
            pararTudo()
            return START_NOT_STICKY
        }

        /*
         * O tipo `location` no startForeground é o que autoriza ler a posição
         * com o app fora da tela. Sem ele, o Android 14+ recusa o serviço; em
         * versões anteriores ele sobe, mas entrega coordenadas congeladas.
         */
        ServiceCompat.startForeground(
            this,
            ID_NOTIFICACAO,
            construirNotificacao(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            } else {
                0
            },
        )

        /*
         * O mapa vem no Intent quando quem liga o rastreio é o check-in. Ligado
         * pelo botão da tela de Rastreio, vem nulo — e nulo é um estado
         * legítimo: o guia pode começar a gravar antes do primeiro embarque.
         */
        if (intent?.hasExtra(EXTRA_MAPA) == true) {
            val mapa = intent.getIntExtra(EXTRA_MAPA, -1).takeIf { it > 0 }
            EstadoRastreio.ligado(this, mapa)
        } else if (!EstadoRastreio.ativo(this)) {
            EstadoRastreio.ligado(this, null)
        }

        iniciarCaptura()
        EnvioWorker.agendar(this)

        /*
         * START_STICKY: se o sistema matar o processo por memória, ele recria o
         * serviço. É o que faz o rastreio voltar sozinho num aparelho apertado.
         */
        return START_STICKY
    }

    private fun iniciarCaptura() {
        val pedido = LocationRequest.Builder(INTERVALO_MS)
            /*
             * Alta precisão: é GPS de verdade. Consome mais bateria, e é o que
             * a função pede — um traçado por antena de celular erra quarteirões
             * e não serve para dizer por onde a van passou.
             */
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            // Não adianta receber mais rápido que isso; só gastaria bateria.
            .setMinUpdateIntervalMillis(INTERVALO_MS)
            /*
             * Van parada no ponto não gera pontos repetidos: sem deslocamento
             * mínimo, uma espera de 20 minutos viraria 80 leituras iguais.
             */
            .setMinUpdateDistanceMeters(DESLOCAMENTO_MINIMO_M)
            .build()

        try {
            cliente.requestLocationUpdates(pedido, recebedor, mainLooper)
        } catch (erro: SecurityException) {
            // Permissão revogada com o serviço no ar. Sem ela não há o que
            // fazer aqui — a tela cuida de pedir de novo.
            pararTudo()
        }
    }

    private fun pararTudo() {
        EstadoRastreio.desligado(this)
        cliente.removeLocationUpdates(recebedor)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        cliente.removeLocationUpdates(recebedor)
        super.onDestroy()
    }

    private fun criarCanal() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val canal = NotificationChannel(
            CANAL,
            getString(R.string.canal_rastreio),
            // Baixa importância: informa sem tocar som nem vibrar o dia todo.
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.canal_rastreio_descricao)
            setShowBadge(false)
        }

        getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    private fun construirNotificacao(): Notification {
        val abrir = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )

        val texto = if (capturadas == 0) {
            getString(R.string.rastreio_aguardando_sinal)
        } else {
            getString(R.string.rastreio_pontos, capturadas)
        }

        return NotificationCompat.Builder(this, CANAL)
            .setContentTitle(getString(R.string.rastreio_ativo))
            .setContentText(texto)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(abrir)
            // Sem timestamp e sem som: fica discreta na barra o dia inteiro.
            .setShowWhen(false)
            .setOngoing(true)
            .build()
    }

    private fun atualizarNotificacao() {
        getSystemService(NotificationManager::class.java)
            .notify(ID_NOTIFICACAO, construirNotificacao())
    }

    companion object {
        private const val CANAL = "rastreio"
        private const val ID_NOTIFICACAO = 1

        /** Uma leitura a cada 15s: bom traçado de rota sem fritar a bateria. */
        private const val INTERVALO_MS = 15_000L

        /** Abaixo disso é a van parada, ou o erro do próprio GPS. */
        private const val DESLOCAMENTO_MINIMO_M = 10f

        /** ~5 minutos de captura entre um empurrão e outro na fila. */
        private const val PONTOS_ENTRE_ENVIOS = 20

        const val ACAO_PARAR = "br.com.rio40graus.guiascale.PARAR"

        const val EXTRA_MAPA = "mapa_id"

        /**
         * @param mapaId o mapa de embarque em curso, quando já se sabe. É o
         *   check-in que costuma trazê-lo; pelo botão da tela vem nulo.
         */
        fun iniciar(contexto: Context, mapaId: Int? = null) {
            val intencao = Intent(contexto, RastreioService::class.java).apply {
                if (mapaId != null) putExtra(EXTRA_MAPA, mapaId)
            }
            contexto.startForegroundService(intencao)
        }

        fun parar(contexto: Context) {
            val intencao = Intent(contexto, RastreioService::class.java).apply {
                action = ACAO_PARAR
            }
            contexto.startService(intencao)
        }
    }
}
