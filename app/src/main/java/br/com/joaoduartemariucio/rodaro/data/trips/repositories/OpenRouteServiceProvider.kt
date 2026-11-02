package br.com.joaoduartemariucio.rodaro.data.trips.repositories

import br.com.joaoduartemariucio.rodaro.data.network.DirectionsRequest
import br.com.joaoduartemariucio.rodaro.data.network.DirectionsRoute
import br.com.joaoduartemariucio.rodaro.data.network.OrsApi
import br.com.joaoduartemariucio.rodaro.data.network.Polyline
import br.com.joaoduartemariucio.rodaro.data.network.RodaroApi
import br.com.joaoduartemariucio.rodaro.data.network.RodaroCoordinate
import br.com.joaoduartemariucio.rodaro.data.network.RodaroRouteOption
import br.com.joaoduartemariucio.rodaro.data.network.RodaroRouteRequest
import br.com.joaoduartemariucio.rodaro.domain.common.errors.RouteLookupError
import br.com.joaoduartemariucio.rodaro.domain.trips.entities.PlaceSuggestion
import br.com.joaoduartemariucio.rodaro.domain.trips.entities.RouteEstimate
import br.com.joaoduartemariucio.rodaro.domain.trips.services.RouteEstimateProvider
import br.com.joaoduartemariucio.rodaro.domain.vehicles.entities.VehicleKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class OpenRouteServiceProvider(
    private val api: OrsApi,
    private val routes: RodaroApi? = null,
) : RouteEstimateProvider {
    override suspend fun suggestions(query: String): List<PlaceSuggestion> =
        try {
            api
                .autocomplete(query.trim())
                .features
                .mapNotNull { feature ->
                    feature.properties?.label ?: feature.properties?.name
                }.distinct()
                .map(::PlaceSuggestion)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            emptyList()
        }

    override suspend fun estimate(
        origin: String,
        destination: String,
        vehicleKind: VehicleKind,
    ): RouteEstimate =
        withContext(Dispatchers.IO) {
            val from = coordinates(origin)
            val to = coordinates(destination)
            routes?.let { estimateWithTolls(it, from, to, vehicleKind) } ?: estimateWithoutTollCost(from, to)
        }

    private suspend fun estimateWithoutTollCost(
        from: List<Double>,
        to: List<Double>,
    ): RouteEstimate {
        val response =
            request {
                api.directions(
                    DirectionsRequest(
                        coordinates = listOf(from, to),
                        extraInfo = listOf("tollways", "waycategory"),
                        geometry = true,
                    ),
                )
            }
        val route = response.routes.firstOrNull() ?: throw RouteLookupError.RequestFailed(IllegalStateException("empty route"))
        return RouteEstimate(
            distanceKm = route.summary.distance / METERS_PER_KILOMETER,
            durationMinutes = (route.summary.duration / SECONDS_PER_MINUTE).roundToLong(),
            hasTolls = hasTolls(route),
            path = route.geometry?.let { Polyline.decode(it, Polyline.PRECISION_5) }.orEmpty(),
        )
    }

    private suspend fun estimateWithTolls(
        routes: RodaroApi,
        from: List<Double>,
        to: List<Double>,
        vehicleKind: VehicleKind,
    ): RouteEstimate {
        val response =
            request {
                routes.routes(
                    RodaroRouteRequest(
                        origin = coordinate(from),
                        destination = coordinate(to),
                        vehicle = if (vehicleKind == VehicleKind.MOTORCYCLE) "motorcycle" else "car",
                    ),
                )
            }
        val outbound = response.outbound.firstOrNull() ?: throw RouteLookupError.RequestFailed(IllegalStateException("empty route"))
        val inbound = response.inbound?.firstOrNull()
        val tollCost = tollCost(outbound)
        return RouteEstimate(
            distanceKm = outbound.distanceKm,
            durationMinutes = outbound.durationMinutes,
            hasTolls = outbound.tolls.charges.isNotEmpty(),
            tollCost = tollCost,
            roundTripTollCost = inbound?.let { tollCost.add(tollCost(it)) } ?: tollCost.multiply(BigDecimal(2)),
            path = Polyline.decode(outbound.geometry, Polyline.PRECISION_6),
        )
    }

    private fun tollCost(option: RodaroRouteOption): BigDecimal = BigDecimal.valueOf(option.tolls.total).setScale(2, RoundingMode.HALF_UP)

    private fun coordinate(lonLat: List<Double>) = RodaroCoordinate(lat = lonLat[1], lon = lonLat[0])

    private suspend fun <T> request(call: suspend () -> T): T =
        try {
            call()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: HttpException) {
            throw httpAsLookupError(error)
        } catch (error: UnknownHostException) {
            throw RouteLookupError.NetworkUnavailable
        } catch (error: SocketTimeoutException) {
            throw RouteLookupError.NetworkUnavailable
        } catch (error: IOException) {
            throw RouteLookupError.NetworkUnavailable
        } catch (error: Exception) {
            throw RouteLookupError.RequestFailed(error)
        }

    private suspend fun coordinates(text: String): List<Double> =
        try {
            api
                .geocode(text)
                .features
                .firstOrNull()
                ?.geometry
                ?.coordinates
                ?.takeIf { it.size == 2 }
                ?: throw RouteLookupError.PlaceNotFound(text)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (lookup: RouteLookupError) {
            throw lookup
        } catch (error: HttpException) {
            throw httpAsLookupError(error)
        } catch (error: UnknownHostException) {
            throw RouteLookupError.NetworkUnavailable
        } catch (error: SocketTimeoutException) {
            throw RouteLookupError.NetworkUnavailable
        } catch (error: IOException) {
            throw RouteLookupError.NetworkUnavailable
        } catch (error: Exception) {
            throw RouteLookupError.RequestFailed(error)
        }

    private fun httpAsLookupError(error: HttpException): RouteLookupError =
        when (error.code()) {
            401, 403 -> RouteLookupError.InvalidApiKey
            429 -> RouteLookupError.QuotaExceeded
            else -> RouteLookupError.RequestFailed(error)
        }

    companion object {
        private const val METERS_PER_KILOMETER = 1_000.0
        private const val SECONDS_PER_MINUTE = 60.0
        private const val TOLLWAY = 1
        private const val WAYCATEGORY_TOLLWAY_FLAG = 2
    }

    private fun hasTolls(route: DirectionsRoute): Boolean {
        val extras = route.extras ?: return false
        val tollways =
            extras.tollways
                ?.summary
                .orEmpty()
                .any { it.value.roundToInt() == TOLLWAY }
        val waycategorySummary =
            extras.waycategory
                ?.summary
                .orEmpty()
                .any { (it.value.roundToInt() and WAYCATEGORY_TOLLWAY_FLAG) != 0 }
        val waycategoryValues =
            extras.waycategory?.values.orEmpty().any { section ->
                section.getOrNull(2)?.let {
                    (
                        it and
                            WAYCATEGORY_TOLLWAY_FLAG
                    ) !=
                        0
                } ==
                    true
            }
        return tollways || waycategorySummary || waycategoryValues
    }
}
