package br.com.joaoduartemariucio.rodaro.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeocodeResponse(
    val features: List<GeocodeFeature> = emptyList(),
)

@Serializable
data class GeocodeFeature(
    val geometry: GeocodeGeometry,
    val properties: GeocodeProperties? = null,
)

@Serializable
data class GeocodeProperties(
    val label: String? = null,
    val name: String? = null,
)

@Serializable
data class GeocodeGeometry(
    val coordinates: List<Double> = emptyList(),
)

@Serializable
data class DirectionsRequest(
    val coordinates: List<List<Double>>,
    @SerialName("extra_info")
    val extraInfo: List<String> = emptyList(),
    val instructions: Boolean = false,
    val geometry: Boolean = false,
)

@Serializable
data class DirectionsResponse(
    val routes: List<DirectionsRoute> = emptyList(),
)

@Serializable
data class DirectionsRoute(
    val summary: DirectionsSummary,
    val geometry: String? = null,
    val extras: DirectionsExtras? = null,
)

@Serializable
data class DirectionsSummary(
    val distance: Double,
    val duration: Double,
)

@Serializable
data class DirectionsExtras(
    val tollways: TollwaysExtra? = null,
    val waycategory: WayCategoryExtra? = null,
)

@Serializable
data class TollwaysExtra(
    val summary: List<TollwaySegment> = emptyList(),
)

@Serializable
data class WayCategoryExtra(
    val values: List<List<Int>> = emptyList(),
    val summary: List<TollwaySegment> = emptyList(),
)

@Serializable
data class TollwaySegment(
    val value: Double,
)
