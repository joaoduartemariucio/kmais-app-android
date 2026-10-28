package br.com.joaoduartemariucio.rodaro.data.reminders.services

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import br.com.joaoduartemariucio.rodaro.domain.reminders.services.Reminder
import br.com.joaoduartemariucio.rodaro.domain.reminders.services.ReminderScheduler
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

class WorkManagerReminderScheduler(
    context: Context,
    private val permission: NotificationPermission,
    private val clock: () -> Instant = Instant::now,
) : ReminderScheduler {
    private val workManager = WorkManager.getInstance(context)

    override suspend fun requestAuthorization(): Boolean = permission.request()

    override suspend fun schedule(reminder: Reminder) {
        val delay = Duration.between(clock(), reminder.fireDate).toMillis().coerceAtLeast(0)
        val request =
            OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(
                    workDataOf(
                        ReminderWorker.KEY_ID to reminder.id.toString(),
                        ReminderWorker.KEY_TITLE to reminder.title,
                        ReminderWorker.KEY_BODY to reminder.body,
                    ),
                ).addTag(TAG)
                .addTag(TAG_PREFIX + reminder.id)
                .build()
        workManager.enqueueUniqueWork(reminder.id.toString(), ExistingWorkPolicy.REPLACE, request)
    }

    override suspend fun cancel(id: UUID) {
        workManager.cancelUniqueWork(id.toString())
    }

    override suspend fun pendingIdentifiers(): List<UUID> =
        workManager
            .getWorkInfosByTagFlow(TAG)
            .first()
            .filter { it.state == WorkInfo.State.ENQUEUED }
            .flatMap { info -> info.tags.filter { it.startsWith(TAG_PREFIX) } }
            .mapNotNull { runCatching { UUID.fromString(it.removePrefix(TAG_PREFIX)) }.getOrNull() }
            .distinct()

    companion object {
        const val TAG = "service-reminder"
        const val TAG_PREFIX = "service-reminder:"
    }
}
