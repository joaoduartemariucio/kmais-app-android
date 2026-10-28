package br.com.joaoduartemariucio.rodaro.domain.maintenance.calculations

import java.time.Instant

data class MaintenanceStatus(
    val level: Level,
    val progress: Double,
    val remainingKilometers: Double?,
    val remainingDays: Int?,
    val overdueBy: MaintenanceForecast.Trigger? = null,
    val calendarDueDate: Instant? = null,
) {
    enum class Level {
        ON_TRACK,
        DUE_SOON,
        OVERDUE,
    }

    val isEstimatePassed: Boolean get() = level != Level.OVERDUE && (remainingDays ?: 0) < 0
}
