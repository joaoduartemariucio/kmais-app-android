package br.com.joaoduartemariucio.rodaro.data.network

import kotlinx.serialization.Serializable

@Serializable
data class RodaroCoordinate(
    val lat: Double,
    val lon: Double,
)

@Serializable
data class RodaroRouteRequest(
    val origin: RodaroCoordinate,
    val destination: RodaroCoordinate,
    val vehicle: String,
    val roundTrip: Boolean = true,
    val alternatives: Int = 0,
)

@Serializable
data class RodaroRoutesResponse(
    val outbound: List<RodaroRouteOption> = emptyList(),
    val inbound: List<RodaroRouteOption>? = null,
)

@Serializable
data class RodaroRouteOption(
    val distanceKm: Double,
    val durationMinutes: Long,
    val geometry: String = "",
    val tolls: RodaroTolls,
)

@Serializable
data class RodaroTolls(
    val total: Double,
    val complete: Boolean = true,
    val charges: List<RodaroTollCharge> = emptyList(),
)

@Serializable
data class RodaroTollCharge(
    val name: String? = null,
    val value: Double? = null,
)
