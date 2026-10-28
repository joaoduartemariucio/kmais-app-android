package br.com.joaoduartemariucio.rodaro.domain.reminders.calculations

import br.com.joaoduartemariucio.rodaro.domain.maintenance.calculations.MaintenanceStatus
import br.com.joaoduartemariucio.rodaro.domain.maintenance.calculations.ServiceDue
import br.com.joaoduartemariucio.rodaro.domain.vehicles.entities.Vehicle
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
