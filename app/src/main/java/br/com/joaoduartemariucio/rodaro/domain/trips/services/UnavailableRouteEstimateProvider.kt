package br.com.joaoduartemariucio.rodaro.domain.trips.services

import br.com.joaoduartemariucio.rodaro.domain.common.errors.RouteLookupError
import br.com.joaoduartemariucio.rodaro.domain.trips.entities.PlaceSuggestion
import br.com.joaoduartemariucio.rodaro.domain.trips.entities.RouteEstimate
import br.com.joaoduartemariucio.rodaro.domain.vehicles.entities.VehicleKind

object UnavailableRouteEstimateProvider : RouteEstimateProvider {
    override suspend fun suggestions(query: String): List<PlaceSuggestion> = emptyList()

    override suspend fun estimate(
        origin: String,
        destination: String,
        vehicleKind: VehicleKind,
    ): RouteEstimate = throw RouteLookupError.NoApiKey
}
