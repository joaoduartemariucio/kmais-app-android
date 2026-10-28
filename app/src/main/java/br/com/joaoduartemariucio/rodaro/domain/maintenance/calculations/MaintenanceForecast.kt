package br.com.joaoduartemariucio.rodaro.domain.maintenance.calculations

import java.time.Instant

data class MaintenanceForecast(
    val dueDate: Instant?,
    val trigger: Trigger,
    val dueOdometer: Double?,
    val averageKilometersPerMonth: Double?,
) {
    enum class Trigger {
        DISTANCE,
        CALENDAR,
    }
}
