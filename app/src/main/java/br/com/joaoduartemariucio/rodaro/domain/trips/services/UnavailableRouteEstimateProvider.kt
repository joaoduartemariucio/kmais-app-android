package br.com.joaoduartemariucio.rodaro.domain.trips.services

import br.com.joaoduartemariucio.rodaro.domain.common.errors.RouteLookupError
import br.com.joaoduartemariucio.rodaro.domain.trips.entities.RouteEstimate

object UnavailableRouteEstimateProvider : RouteEstimateProvider {
    override suspend fun estimate(
        origin: String,
        destination: String,
    ): RouteEstimate = throw RouteLookupError.NoApiKey
}
