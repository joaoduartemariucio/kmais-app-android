package br.com.joaoduartemariucio.rodaro.domain.maintenance.calculations

import br.com.joaoduartemariucio.rodaro.domain.maintenance.entities.ServiceRule
import br.com.joaoduartemariucio.rodaro.domain.vehicles.entities.OdometerSample

data class ServiceDue(
    val rule: ServiceRule,
    val forecast: MaintenanceForecast,
    val since: OdometerSample,
)
