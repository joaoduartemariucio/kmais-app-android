package br.com.joaoduartemariucio.kmais_app_android.data.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import br.com.joaoduartemariucio.kmais_app_android.R

class ReminderWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val id = inputData.getString(KEY_ID) ?: return Result.failure()
        if (!NotificationPermission(applicationContext).isGranted) return Result.success()

        val manager = NotificationManagerCompat.from(applicationContext)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                applicationContext.getString(R.string.notification_channel_reminders),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )

        val launch = applicationContext.packageManager.getLaunchIntentForPackage(applicationContext.packageName)
        val notification =
            NotificationCompat
                .Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(inputData.getString(KEY_TITLE))
                .setContentText(inputData.getString(KEY_BODY))
                .setStyle(NotificationCompat.BigTextStyle().bigText(inputData.getString(KEY_BODY)))
                .setAutoCancel(true)
                .setContentIntent(
                    launch?.let {
                        PendingIntent.getActivity(applicationContext, 0, it, PendingIntent.FLAG_IMMUTABLE)
                    },
                ).build()

        try {
            manager.notify(id.hashCode(), notification)
        } catch (_: SecurityException) {
            return Result.success()
        }
        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "service-reminders"
        const val KEY_ID = "id"
        const val KEY_TITLE = "title"
        const val KEY_BODY = "body"
    }
}
