package br.com.joaoduartemariucio.kmais_app_android.app

import br.com.joaoduartemariucio.kmais_app_android.domain.entities.Vehicle

sealed interface AppRoute {
    data object Vehicles : AppRoute

    data class VehicleDetail(
        val vehicle: Vehicle,
    ) : AppRoute

    data object ArchivedVehicles : AppRoute
}
