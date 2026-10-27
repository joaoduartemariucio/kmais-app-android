package br.com.joaoduartemariucio.kmais_app_android.domain.calculations

import br.com.joaoduartemariucio.kmais_app_android.domain.entities.FuelEntry
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.ServiceEntry

object Cost {
    fun summary(
        fuel: List<FuelEntry>,
        services: List<ServiceEntry>,
    ): CostSummary {
        val odometers = fuel.map { it.odometer } + services.map { it.odometer }
        val lowest = odometers.minOrNull()
        val highest = odometers.maxOrNull()
        val distance = if (lowest != null && highest != null) maxOf(0.0, highest - lowest) else 0.0

        return CostSummary(
            fuelCost = fuel.map { it.totalCost }.sum(),
            serviceCost = services.map { it.cost }.sum(),
            distance = distance,
        )
    }
}
