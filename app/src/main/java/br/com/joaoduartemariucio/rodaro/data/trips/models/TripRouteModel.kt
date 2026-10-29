package br.com.joaoduartemariucio.rodaro.data.trips.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity(tableName = "trip_routes")
data class TripRouteModel(
    @PrimaryKey val id: UUID,
    val vehicleId: UUID,
    val origin: String,
    val destination: String,
    val distanceKm: Double,
    val isRoundTrip: Boolean,
    val hasTolls: Boolean,
    val liters: Double,
    val fuelCost: BigDecimal,
    val costPerKilometer: BigDecimal,
    val createdAt: Instant,
)
