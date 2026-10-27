package br.com.joaoduartemariucio.kmais_app_android.domain.calculations

import br.com.joaoduartemariucio.kmais_app_android.domain.entities.Vehicle
import java.time.Instant
import java.util.UUID

data class PlannedReminder(
    val vehicle: Vehicle,
    val due: ServiceDue,
    val status: MaintenanceStatus,
    val currentOdometer: Double,
    val fireDate: Instant,
) {
    val id: UUID get() = due.rule.id
    val isOverdue: Boolean get() = status.level == MaintenanceStatus.Level.OVERDUE
}
