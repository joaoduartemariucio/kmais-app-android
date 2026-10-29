package br.com.joaoduartemariucio.rodaro.presentation.trips

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import br.com.joaoduartemariucio.rodaro.app.NoAnalytics
import br.com.joaoduartemariucio.rodaro.domain.common.entities.sortedChronologically
import br.com.joaoduartemariucio.rodaro.domain.common.services.Analytics
import br.com.joaoduartemariucio.rodaro.domain.common.services.DateProvider
import br.com.joaoduartemariucio.rodaro.domain.fueling.calculations.Consumption
import br.com.joaoduartemariucio.rodaro.domain.fueling.repositories.FuelEntryRepository
import br.com.joaoduartemariucio.rodaro.domain.trips.calculations.TripCost
import br.com.joaoduartemariucio.rodaro.domain.trips.entities.SavedRoute
import br.com.joaoduartemariucio.rodaro.domain.trips.repositories.RouteRepository
import br.com.joaoduartemariucio.rodaro.domain.trips.services.RouteEstimateProvider
import br.com.joaoduartemariucio.rodaro.domain.trips.services.UnavailableRouteEstimateProvider
import br.com.joaoduartemariucio.rodaro.domain.vehicles.entities.Vehicle
import br.com.joaoduartemariucio.rodaro.domain.vehicles.repositories.VehicleRepository
import br.com.joaoduartemariucio.rodaro.presentation.common.NumberInput
import br.com.joaoduartemariucio.rodaro.presentation.common.UnitConversion
import java.math.BigDecimal
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

class TripCostViewModel(
    private val vehicleRepository: VehicleRepository,
    private val fuelEntryRepository: FuelEntryRepository,
    private val routeRepository: RouteRepository,
    private val routeEstimateProvider: RouteEstimateProvider = UnavailableRouteEstimateProvider,
    private val dateProvider: DateProvider,
    private val analytics: Analytics = NoAnalytics,
) : ViewModel() {
    sealed interface State {
        data object Loading : State

        data object Loaded : State

        data class Failed(
            val error: Throwable,
        ) : State
    }

    sealed interface LookupState {
        data object Idle : LookupState

        data object Working : LookupState

        data object Done : LookupState

        data class Failed(
            val error: Throwable,
        ) : LookupState
    }

    var state: State by mutableStateOf(State.Loading)
        private set

    var vehicles: List<Vehicle> by mutableStateOf(emptyList())
        private set

    var selectedVehicle: Vehicle? by mutableStateOf(null)
        private set

    var savedRoutes: List<SavedRoute> by mutableStateOf(emptyList())
        private set

    var distance by mutableStateOf("")
    var isRoundTrip by mutableStateOf(false)
    var consumption by mutableStateOf("")
    var pricePerLiter by mutableStateOf("")
    var origin by mutableStateOf("")
    var destination by mutableStateOf("")
    var hasTolls: Boolean? by mutableStateOf(null)

    var lookupState: LookupState by mutableStateOf(LookupState.Idle)
        private set

    var saving by mutableStateOf(false)
        private set

    var saveError: Throwable? by mutableStateOf(null)
        private set

    private var suggestedConsumption: String? by mutableStateOf(null)
    private var suggestedPrice: String? by mutableStateOf(null)

    val parsedDistance: Double?
        get() = NumberInput.double(distance, NumberInput.Style.KILOMETERS)?.let(UnitConversion::distanceToKilometers)

    val parsedConsumption: Double?
        get() = NumberInput.double(consumption, NumberInput.Style.CONSUMPTION)?.let(UnitConversion::consumptionToKilometersPerLiter)

    val parsedPrice: BigDecimal?
        get() = NumberInput.decimal(pricePerLiter, NumberInput.Style.FUEL_PRICE)?.let(UnitConversion::perVolumeToLiters)

    val isUsingAverageConsumption: Boolean get() = suggestedConsumption != null && consumption == suggestedConsumption
    val isUsingLastPrice: Boolean get() = suggestedPrice != null && pricePerLiter == suggestedPrice

    val canLookupRealDistance: Boolean get() = origin.isNotBlank() && destination.isNotBlank() && lookupState !is LookupState.Working

    val canSaveRoute: Boolean get() = cost != null && selectedVehicle != null && canSaveInputs && !saving

    private val canSaveInputs: Boolean get() = origin.isNotBlank() && destination.isNotBlank() && parsedDistance != null

    fun reportCalculation() {
        analytics.event(
            "route_calculated",
            mapOf(
                "round_trip" to isRoundTrip.toString(),
                "has_vehicle" to (selectedVehicle != null).toString(),
            ),
        )
    }

    val cost: TripCost?
        get() {
            val distance = parsedDistance ?: return null
            val consumption = parsedConsumption ?: return null
            val price = parsedPrice ?: return null
            return TripCost.of(distance, isRoundTrip, consumption, price)
        }

    suspend fun load() {
        try {
            vehicles = vehicleRepository.all(includingArchived = false).filter { !it.isArchived }
            savedRoutes = routeRepository.routes()
            val current = selectedVehicle?.let { selected -> vehicles.firstOrNull { it.id == selected.id } }
            state = State.Loaded
            val next = current ?: vehicles.firstOrNull()
            when {
                next == null -> clearSuggestions()
                next != selectedVehicle -> select(next)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            state = State.Failed(error)
        }
    }

    suspend fun select(vehicle: Vehicle) {
        selectedVehicle = vehicle
        analytics.event("route_vehicle_selected")
        try {
            val fuel = fuelEntryRepository.entries(vehicle.id)
            val average = Consumption.average(fuel)
            val lastPrice = fuel.sortedChronologically().lastOrNull()?.pricePerLiter
            applySuggestions(
                consumption = average?.let { NumberInput.text(UnitConversion.consumption(it), NumberInput.Style.CONSUMPTION) },
                price = lastPrice?.let { NumberInput.text(UnitConversion.perVolume(it), NumberInput.Style.FUEL_PRICE) },
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            state = State.Failed(error)
        }
    }

    suspend fun lookupRealDistance() {
        if (!canLookupRealDistance) return
        lookupState = LookupState.Working
        try {
            val estimate = routeEstimateProvider.estimate(origin.trim(), destination.trim())
            distance = NumberInput.text(UnitConversion.distance(estimate.distanceKm), NumberInput.Style.KILOMETERS)
            isRoundTrip = false
            hasTolls = estimate.hasTolls
            lookupState = LookupState.Done
            analytics.event(
                "route_distance_looked_up",
                mapOf("has_tolls" to estimate.hasTolls.toString()),
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            lookupState = LookupState.Failed(error)
        }
    }

    suspend fun saveRoute(): Boolean {
        val vehicle = selectedVehicle ?: return false
        val cost = cost ?: return false
        if (!canSaveInputs) return false
        saving = true
        saveError = null
        return try {
            routeRepository.save(
                SavedRoute(
                    id = UUID.randomUUID(),
                    vehicleId = vehicle.id,
                    origin = origin.trim(),
                    destination = destination.trim(),
                    distanceKm = parsedDistance ?: return false,
                    isRoundTrip = isRoundTrip,
                    hasTolls = hasTolls ?: false,
                    liters = cost.liters,
                    fuelCost = cost.fuelCost,
                    costPerKilometer = cost.costPerKilometer,
                    createdAt = dateProvider.now,
                ),
            )
            savedRoutes = routeRepository.routes()
            analytics.event(
                "route_saved",
                mapOf(
                    "round_trip" to isRoundTrip.toString(),
                    "has_tolls" to (hasTolls ?: false).toString(),
                ),
            )
            true
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            saveError = error
            false
        } finally {
            saving = false
        }
    }

    suspend fun deleteRoute(id: UUID) {
        try {
            routeRepository.delete(id)
            savedRoutes = routeRepository.routes()
            analytics.event("route_deleted")
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            saveError = error
        }
    }

    fun restoreRoute(route: SavedRoute) {
        origin = route.origin
        destination = route.destination
        distance = NumberInput.text(UnitConversion.distance(route.distanceKm), NumberInput.Style.KILOMETERS)
        isRoundTrip = route.isRoundTrip
        hasTolls = route.hasTolls
    }

    private fun applySuggestions(
        consumption: String?,
        price: String?,
    ) {
        if (this.consumption.isEmpty() || isUsingAverageConsumption) this.consumption = consumption.orEmpty()
        if (pricePerLiter.isEmpty() || isUsingLastPrice) pricePerLiter = price.orEmpty()
        suggestedConsumption = consumption
        suggestedPrice = price
    }

    private fun clearSuggestions() {
        applySuggestions(consumption = null, price = null)
        selectedVehicle = null
    }
}
