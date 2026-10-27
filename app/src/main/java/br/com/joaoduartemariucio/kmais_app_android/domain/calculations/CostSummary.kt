package br.com.joaoduartemariucio.kmais_app_android.domain.calculations

import java.math.BigDecimal
import java.math.MathContext

data class CostSummary(
    val fuelCost: BigDecimal,
    val serviceCost: BigDecimal,
    val distance: Double,
) {
    val totalCost: BigDecimal get() = fuelCost + serviceCost

    val costPerKilometer: BigDecimal?
        get() = if (distance > 0) totalCost.divide(distance.toDecimal(), MathContext.DECIMAL128) else null
}
