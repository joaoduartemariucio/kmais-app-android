package br.com.joaoduartemariucio.kmais_app_android.domain.entities

data class VehicleRecords(
    val vehicle: Vehicle,
    val fuel: List<FuelEntry>,
    val services: List<ServiceEntry>,
    val rules: List<ServiceRule>,
)
