package br.com.joaoduartemariucio.rodaro.data.trips.repositories

import br.com.joaoduartemariucio.rodaro.data.network.DirectionsRequest
import br.com.joaoduartemariucio.rodaro.data.network.OrsApi
import br.com.joaoduartemariucio.rodaro.domain.common.errors.RouteLookupError
import br.com.joaoduartemariucio.rodaro.domain.trips.entities.RouteEstimate
import br.com.joaoduartemariucio.rodaro.domain.trips.services.RouteEstimateProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToLong

class OpenRouteServiceProvider(
    private val api: OrsApi,
) : RouteEstimateProvider {
    override suspend fun estimate(
        origin: String,
        destination: String,
    ): RouteEstimate =
        withContext(Dispatchers.IO) {
            val from = coordinates(origin)
            val to = coordinates(destination)
            val response = directions(from, to)
            val route = response.routes.firstOrNull() ?: throw RouteLookupError.RequestFailed(IllegalStateException("empty route"))
            RouteEstimate(
                distanceKm = route.summary.distance / METERS_PER_KILOMETER,
                durationMinutes = (route.summary.duration / SECONDS_PER_MINUTE).roundToLong(),
                hasTolls =
                    route.extras
                        ?.tollways
                        ?.summary
                        .orEmpty()
                        .any { it.value == TOLLWAY },
            )
        }

    private suspend fun directions(
        from: List<Double>,
        to: List<Double>,
    ) = try {
        api.directions(
            DirectionsRequest(
                coordinates = listOf(from, to),
                extraInfo = listOf("tollways"),
            ),
        )
    } catch (cancellation: CancellationException) {
        throw cancellation
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
            throw RouteLookupError.RequestFailed(error)
        } catch (error: Exception) {
            throw RouteLookupError.RequestFailed(error)
        }

    companion object {
        private const val METERS_PER_KILOMETER = 1_000.0
        private const val SECONDS_PER_MINUTE = 60.0
        private const val TOLLWAY = 1
    }
}
