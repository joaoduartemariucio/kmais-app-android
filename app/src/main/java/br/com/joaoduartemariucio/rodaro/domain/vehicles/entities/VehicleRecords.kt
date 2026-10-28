package br.com.joaoduartemariucio.rodaro.domain.vehicles.entities

import br.com.joaoduartemariucio.rodaro.domain.fueling.entities.FuelEntry
import br.com.joaoduartemariucio.rodaro.domain.maintenance.entities.ServiceEntry
import br.com.joaoduartemariucio.rodaro.domain.maintenance.entities.ServiceRule

data class VehicleRecords(
    val vehicle: Vehicle,
    val fuel: List<FuelEntry>,
    val services: List<ServiceEntry>,
    val rules: List<ServiceRule>,
)
