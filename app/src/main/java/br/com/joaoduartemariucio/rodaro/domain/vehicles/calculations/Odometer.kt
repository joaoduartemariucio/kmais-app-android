package br.com.joaoduartemariucio.rodaro.domain.vehicles.calculations

import br.com.joaoduartemariucio.rodaro.domain.common.entities.sortedChronologically
import br.com.joaoduartemariucio.rodaro.domain.fueling.entities.FuelEntry
import br.com.joaoduartemariucio.rodaro.domain.maintenance.entities.ServiceEntry
import br.com.joaoduartemariucio.rodaro.domain.vehicles.entities.OdometerSample
import br.com.joaoduartemariucio.rodaro.domain.vehicles.entities.Vehicle
import java.time.Instant

object Odometer {
    fun current(samples: List<OdometerSample>): Double? = samples.sortedChronologically().lastOrNull()?.odometer

    fun neighbors(
        date: Instant,
        samples: List<OdometerSample>,
    ): OdometerNeighbors {
        val sorted = samples.sortedChronologically()
        return OdometerNeighbors(
            previous = sorted.lastOrNull { it.date <= date }?.odometer,
            next = sorted.firstOrNull { it.date > date }?.odometer,
        )
    }

    fun samples(
        vehicle: Vehicle,
        fuel: List<FuelEntry>,
        services: List<ServiceEntry>,
    ): List<OdometerSample> =
        listOf(OdometerSample(vehicle.createdAt, vehicle.initialOdometer)) +
            fuel.map { OdometerSample(it.date, it.odometer) } +
            services.map { OdometerSample(it.date, it.odometer) }
}
