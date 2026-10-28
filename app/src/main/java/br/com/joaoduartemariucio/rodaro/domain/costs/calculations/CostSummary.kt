package br.com.joaoduartemariucio.rodaro.domain.costs.calculations

import br.com.joaoduartemariucio.rodaro.domain.common.calculations.toDecimal
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
