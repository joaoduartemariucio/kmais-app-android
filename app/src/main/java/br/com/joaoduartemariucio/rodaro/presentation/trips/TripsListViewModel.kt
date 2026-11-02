package br.com.joaoduartemariucio.rodaro.presentation.trips

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import br.com.joaoduartemariucio.rodaro.app.NoAnalytics
import br.com.joaoduartemariucio.rodaro.domain.common.services.Analytics
import br.com.joaoduartemariucio.rodaro.domain.trips.entities.SavedRoute
import br.com.joaoduartemariucio.rodaro.domain.trips.repositories.RouteRepository
import br.com.joaoduartemariucio.rodaro.domain.vehicles.entities.Vehicle
import br.com.joaoduartemariucio.rodaro.domain.vehicles.repositories.VehicleRepository
import java.math.BigDecimal
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

class TripsListViewModel(
    private val routeRepository: RouteRepository,
    private val vehicleRepository: VehicleRepository,
    private val analytics: Analytics = NoAnalytics,
) : ViewModel() {
    sealed interface State {
        data object Loading : State

        data object Loaded : State

        data class Failed(
            val error: Throwable,
        ) : State
    }

    var state: State by mutableStateOf(State.Loading)
        private set

    var routes: List<SavedRoute> by mutableStateOf(emptyList())
        private set

    var vehicles: Map<UUID, Vehicle> by mutableStateOf(emptyMap())
        private set

    var vehicleFilter: UUID? by mutableStateOf(null)
        private set

    var actionError: Throwable? by mutableStateOf(null)
        private set

    val filterVehicles: List<Vehicle>
        get() = routes.map { it.vehicleId }.distinct().mapNotNull { vehicles[it] }

    val visibleRoutes: List<SavedRoute>
        get() = vehicleFilter?.let { id -> routes.filter { it.vehicleId == id } } ?: routes

    val visibleTotalCost: BigDecimal
        get() = visibleRoutes.fold(BigDecimal.ZERO) { total, route -> total + route.totalCost }

    val visibleTotalDistance: Double
        get() = visibleRoutes.sumOf { it.totalDistanceKm }

    fun vehicle(route: SavedRoute): Vehicle? = vehicles[route.vehicleId]

    fun filter(vehicleId: UUID?) {
        vehicleFilter = vehicleId
    }

    suspend fun load() {
        try {
            routes = routeRepository.routes()
            vehicles = vehicleRepository.all(includingArchived = true).associateBy { it.id }
            if (vehicleFilter != null && routes.none { it.vehicleId == vehicleFilter }) vehicleFilter = null
            actionError = null
            state = State.Loaded
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            state = State.Failed(error)
        }
    }

    suspend fun deleteRoute(route: SavedRoute) {
        try {
            routeRepository.delete(route.id)
            routes = routeRepository.routes()
            if (routes.none { it.vehicleId == vehicleFilter }) vehicleFilter = null
            actionError = null
            analytics.event("route_deleted")
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            actionError = error
        }
    }
}
