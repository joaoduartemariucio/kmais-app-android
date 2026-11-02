package br.com.joaoduartemariucio.rodaro.domain.trips.calculations

import kotlin.math.ceil

data class TankPlan(
    val tankCapacity: Double,
    val tanks: Double,
    val rangePerTank: Double,
) {
    val fillUps: Int get() = ceil(tanks - ROUNDING_TOLERANCE).toInt().coerceAtLeast(1)

    val stopsOnTheWay: Int get() = fillUps - 1

    val fitsInOneTank: Boolean get() = fillUps == 1

    companion object {
        private const val ROUNDING_TOLERANCE = 1e-6

        fun of(
            liters: Double,
            kilometersPerLiter: Double,
            tankCapacity: Double?,
        ): TankPlan? {
            if (tankCapacity == null || tankCapacity <= 0 || liters <= 0 || kilometersPerLiter <= 0) return null
            return TankPlan(
                tankCapacity = tankCapacity,
                tanks = liters / tankCapacity,
                rangePerTank = tankCapacity * kilometersPerLiter,
            )
        }
    }
}
