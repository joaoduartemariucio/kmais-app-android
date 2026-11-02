package br.com.joaoduartemariucio.rodaro.domain.trips.entities

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class SavedRoute(
    val id: UUID,
    val vehicleId: UUID,
    val origin: String,
    val destination: String,
    val distanceKm: Double,
    val isRoundTrip: Boolean,
    val hasTolls: Boolean,
    val tollCost: BigDecimal,
    val kilometersPerLiter: Double,
    val pricePerLiter: BigDecimal,
    val liters: Double,
    val fuelCost: BigDecimal,
    val costPerKilometer: BigDecimal,
    val createdAt: Instant,
    val path: List<RoutePoint> = emptyList(),
) {
    val totalDistanceKm: Double get() = if (isRoundTrip) distanceKm * 2 else distanceKm

    val totalCost: BigDecimal get() = fuelCost + tollCost
}
