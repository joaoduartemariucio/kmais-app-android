package br.com.joaoduartemariucio.rodaro.domain.reminders.services

import java.time.Instant
import java.util.UUID

interface ReminderScheduler {
    suspend fun requestAuthorization(): Boolean

    suspend fun schedule(reminder: Reminder)

    suspend fun cancel(id: UUID)

    suspend fun pendingIdentifiers(): List<UUID>
}

data class Reminder(
    val id: UUID,
    val title: String,
    val body: String,
    val fireDate: Instant,
)
