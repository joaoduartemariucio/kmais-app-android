package br.com.joaoduartemariucio.rodaro.presentation.trips

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import br.com.joaoduartemariucio.rodaro.app.NoAnalytics
import br.com.joaoduartemariucio.rodaro.domain.common.services.Analytics
import br.com.joaoduartemariucio.rodaro.domain.trips.calculations.TankPlan
import br.com.joaoduartemariucio.rodaro.domain.trips.entities.SavedRoute
import br.com.joaoduartemariucio.rodaro.domain.trips.repositories.RouteRepository
import br.com.joaoduartemariucio.rodaro.domain.vehicles.entities.Vehicle
import br.com.joaoduartemariucio.rodaro.domain.vehicles.repositories.VehicleRepository
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

class RouteDetailViewModel(
    private val routeRepository: RouteRepository,
    private val vehicleRepository: VehicleRepository,
    private val analytics: Analytics = NoAnalytics,
) : ViewModel() {
    sealed interface State {
        data object Loading : State

        data class Loaded(
            val route: SavedRoute,
        ) : State

        data object Missing : State

        data class Failed(
            val error: Throwable,
        ) : State
    }

    var state: State by mutableStateOf(State.Loading)
        private set

    var vehicle: Vehicle? by mutableStateOf(null)
        private set

    var actionError: Throwable? by mutableStateOf(null)
        private set

    val route: SavedRoute? get() = (state as? State.Loaded)?.route

    val tankPlan: TankPlan?
        get() {
            val route = route ?: return null
            return TankPlan.of(route.liters, route.kilometersPerLiter, vehicle?.tankCapacity)
        }

    suspend fun load(id: UUID) {
        try {
            val route = routeRepository.routes().firstOrNull { it.id == id }
            if (route == null) {
                state = State.Missing
                return
            }
            vehicle = vehicleRepository.vehicle(route.vehicleId)
            state = State.Loaded(route)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            state = State.Failed(error)
        }
    }

    suspend fun delete(): Boolean {
        val route = route ?: return false
        return try {
            routeRepository.delete(route.id)
            actionError = null
            analytics.event("route_deleted")
            true
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            actionError = error
            false
        }
    }
}
