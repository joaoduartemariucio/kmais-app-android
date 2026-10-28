package br.com.joaoduartemariucio.rodaro.domain.fueling.calculations

import java.time.Instant

data class ConsumptionInterval(
    val startDate: Instant,
    val endDate: Instant,
    val startOdometer: Double,
    val endOdometer: Double,
    val liters: Double,
) {
    val distance: Double get() = endOdometer - startOdometer

    val kilometersPerLiter: Double get() = distance / liters
}
