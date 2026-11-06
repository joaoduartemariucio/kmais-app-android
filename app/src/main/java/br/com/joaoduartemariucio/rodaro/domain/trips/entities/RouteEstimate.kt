package br.com.joaoduartemariucio.rodaro.domain.trips.entities

import java.math.BigDecimal

data class RouteEstimate(
    val distanceKm: Double,
    val durationMinutes: Long,
    val hasTolls: Boolean,
    val tollCost: BigDecimal? = null,
    val roundTripTollCost: BigDecimal? = null,
    val path: List<RoutePoint> = emptyList(),
)
