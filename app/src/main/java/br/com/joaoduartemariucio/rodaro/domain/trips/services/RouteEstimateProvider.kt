package br.com.joaoduartemariucio.rodaro.domain.trips.services

import br.com.joaoduartemariucio.rodaro.domain.trips.entities.PlaceSuggestion
import br.com.joaoduartemariucio.rodaro.domain.trips.entities.RouteEstimate
import br.com.joaoduartemariucio.rodaro.domain.vehicles.entities.VehicleKind

interface RouteEstimateProvider {
    suspend fun suggestions(query: String): List<PlaceSuggestion>

    suspend fun estimate(
        origin: String,
        destination: String,
        vehicleKind: VehicleKind = VehicleKind.CAR,
    ): RouteEstimate
}
