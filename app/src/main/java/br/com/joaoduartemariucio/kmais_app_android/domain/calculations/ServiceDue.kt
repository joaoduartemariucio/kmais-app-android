package br.com.joaoduartemariucio.kmais_app_android.domain.calculations

import br.com.joaoduartemariucio.kmais_app_android.domain.entities.OdometerSample
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.ServiceRule

data class ServiceDue(
    val rule: ServiceRule,
    val forecast: MaintenanceForecast,
    val since: OdometerSample,
)
