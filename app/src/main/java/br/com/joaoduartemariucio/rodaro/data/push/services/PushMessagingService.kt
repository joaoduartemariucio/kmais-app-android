package br.com.joaoduartemariucio.rodaro.data.push.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import br.com.joaoduartemariucio.rodaro.R
import br.com.joaoduartemariucio.rodaro.data.preferences.PreferenceGate
import br.com.joaoduartemariucio.rodaro.data.reminders.services.NotificationPermission
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class PushMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) = Unit

    override fun onMessageReceived(message: RemoteMessage) {
        if (!PreferenceGate.preferences(this).pushEnabled) return
        val title = message.notification?.title ?: message.data[KEY_TITLE] ?: return
        val body = message.notification?.body ?: message.data[KEY_BODY]
        if (!NotificationPermission(this).isGranted) return

        createChannel(this)
        val launch = packageManager.getLaunchIntentForPackage(packageName)
        val notification =
            NotificationCompat
                .Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setContentIntent(launch?.let { PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE) })
                .build()

        try {
            NotificationManagerCompat.from(this).notify((message.messageId ?: title).hashCode(), notification)
        } catch (_: SecurityException) {
            return
        }
    }

    companion object {
        const val CHANNEL_ID = "general"
        const val KEY_TITLE = "title"
        const val KEY_BODY = "body"

        fun createChannel(context: Context) {
            NotificationManagerCompat.from(context).createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_general),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            )
        }
    }
}
