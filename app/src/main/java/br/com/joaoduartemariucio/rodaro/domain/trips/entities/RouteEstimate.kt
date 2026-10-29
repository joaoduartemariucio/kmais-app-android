package br.com.joaoduartemariucio.rodaro.domain.trips.entities

data class RouteEstimate(
    val distanceKm: Double,
    val durationMinutes: Long,
    val hasTolls: Boolean,
)
