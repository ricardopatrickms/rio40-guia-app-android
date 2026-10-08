package br.com.rio40graus.guiascale.rastreio

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Quem esvazia a fila de posições.
 *
 * Roda pelo WorkManager, e não dentro do serviço, por um motivo prático: o
 * WorkManager sobrevive ao app ser fechado e ao aparelho reiniciar, e só
 * acorda quando há rede. O serviço cuida de capturar; subir é problema daqui.
 *
 * Envia em lotes de até 500 e repete enquanto sobrar coisa na fila — depois de
 * horas sem sinal são milhares de pontos, e uma requisição só não passaria.
 */
class EnvioWorker(
    contexto: Context,
    parametros: WorkerParameters,
) : CoroutineWorker(contexto, parametros) {

    override suspend fun doWork(): Result {
        /*
         * A lógica de subir a fila mora num lugar só — SincronizadorPosicoes —
         * compartilhada com o envio ao vivo do serviço. Aqui é a rede de
         * segurança: roda mesmo com o app fechado e só acorda quando há rede.
         *
         * Falhou (sem conseguir esvaziar tudo): retry, com espera crescente. A
         * fila guarda — ponto nenhum se perde por erro de envio.
         */
        return if (SincronizadorPosicoes.subir(applicationContext)) {
            Result.success()
        } else {
            Result.retry()
        }
    }

    companion object {
        private const val NOME = "envio-de-posicoes"

        /**
         * O envio periódico.
         *
         * 15 minutos é o menor intervalo que o Android aceita para trabalho
         * periódico. O serviço também dispara um envio avulso quando a fila
         * cresce, então na prática o atraso é bem menor — isto aqui é a rede
         * de segurança para quando o app não está capturando.
         */
        fun agendar(contexto: Context) {
            val exigencias = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val trabalho = PeriodicWorkRequestBuilder<EnvioWorker>(15, TimeUnit.MINUTES)
                .setConstraints(exigencias)
                .build()

            WorkManager.getInstance(contexto).enqueueUniquePeriodicWork(
                NOME,
                ExistingPeriodicWorkPolicy.KEEP,
                trabalho,
            )
        }
    }
}
