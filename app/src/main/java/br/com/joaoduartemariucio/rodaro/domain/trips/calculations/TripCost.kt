package br.com.joaoduartemariucio.rodaro.domain.trips.calculations

import br.com.joaoduartemariucio.rodaro.domain.common.calculations.toDecimal
import java.math.BigDecimal
import java.math.MathContext

data class TripCost(
    val distance: Double,
    val liters: Double,
    val fuelCost: BigDecimal,
) {
    val costPerKilometer: BigDecimal get() = fuelCost.divide(distance.toDecimal(), MathContext.DECIMAL128)

    companion object {
        fun of(
            distance: Double,
            isRoundTrip: Boolean,
            kilometersPerLiter: Double,
            pricePerLiter: BigDecimal,
        ): TripCost? {
            if (distance <= 0 || kilometersPerLiter <= 0 || pricePerLiter.signum() < 0) return null
            val total = if (isRoundTrip) distance * 2 else distance
            val liters = total / kilometersPerLiter
            return TripCost(distance = total, liters = liters, fuelCost = liters.toDecimal().multiply(pricePerLiter))
        }
    }
}
