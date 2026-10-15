package br.com.rio40graus.guiascale.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import br.com.rio40graus.guiascale.MainActivity
import br.com.rio40graus.guiascale.R
import com.google.firebase.FirebaseApp

/**
 * Os avisos que chegam por push (mapa alterado, bloqueio aprovado...).
 *
 * Canal separado do de rastreio: aquele é permanente e mudo; este é o que
 * precisa chamar a atenção do guia, com som e na tela.
 */
object Avisos {

    /** Mesmo id do meta-data default_notification_channel_id do manifesto. */
    const val CANAL = "avisos"

    fun criarCanal(contexto: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val canal = NotificationChannel(
            CANAL,
            contexto.getString(R.string.canal_avisos),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = contexto.getString(R.string.canal_avisos_descricao)
        }

        contexto.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(canal)
    }

    /**
     * O homolog não está registrado no Firebase (ver app/build.gradle.kts): lá o
     * FirebaseApp não sobe, e pedir o token derrubaria o app. Todo acesso ao
     * Messaging passa por aqui antes.
     */
    fun firebaseAtivo(contexto: Context): Boolean =
        FirebaseApp.getApps(contexto).isNotEmpty()

    /** Android 13+ pede a permissão; abaixo disso ela vem com a instalação. */
    fun podeNotificar(contexto: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                contexto,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

    fun mostrar(contexto: Context, titulo: String, texto: String, dados: Map<String, String>) {
        if (!podeNotificar(contexto)) return

        // Por ora o toque só abre o app. SINGLE_TOP: se ele já está aberto, volta
        // para a frente como estava, sem recriar a tela. Os dados do push seguem
        // como extras para, no futuro, cada aviso abrir a sua tela.
        val abrir = Intent(contexto, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            dados.forEach { (chave, valor) -> putExtra(chave, valor) }
        }
        val pendente = PendingIntent.getActivity(
            contexto,
            System.currentTimeMillis().toInt(),
            abrir,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notificacao = NotificationCompat.Builder(contexto, CANAL)
            .setSmallIcon(R.drawable.ic_notificacao)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendente)
            .build()

        contexto.getSystemService(NotificationManager::class.java)
            .notify(System.currentTimeMillis().toInt(), notificacao)
    }
}
