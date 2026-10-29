package br.com.joaoduartemariucio.rodaro.domain.trips.services

import br.com.joaoduartemariucio.rodaro.domain.trips.entities.RouteEstimate

interface RouteEstimateProvider {
    suspend fun estimate(
        origin: String,
        destination: String,
    ): RouteEstimate
}
