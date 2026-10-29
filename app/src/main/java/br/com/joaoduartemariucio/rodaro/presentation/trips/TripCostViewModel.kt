package br.com.joaoduartemariucio.rodaro.presentation.trips

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import br.com.joaoduartemariucio.rodaro.domain.common.entities.sortedChronologically
import br.com.joaoduartemariucio.rodaro.domain.fueling.calculations.Consumption
import br.com.joaoduartemariucio.rodaro.domain.fueling.repositories.FuelEntryRepository
import br.com.joaoduartemariucio.rodaro.domain.trips.calculations.TripCost
import br.com.joaoduartemariucio.rodaro.domain.vehicles.entities.Vehicle
import br.com.joaoduartemariucio.rodaro.domain.vehicles.repositories.VehicleRepository
import br.com.joaoduartemariucio.rodaro.presentation.common.NumberInput
import java.math.BigDecimal
import kotlin.coroutines.cancellation.CancellationException

class TripCostViewModel(
    private val vehicleRepository: VehicleRepository,
    private val fuelEntryRepository: FuelEntryRepository,
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

    var vehicles: List<Vehicle> by mutableStateOf(emptyList())
        private set

    var selectedVehicle: Vehicle? by mutableStateOf(null)
        private set

    var distance by mutableStateOf("")
    var isRoundTrip by mutableStateOf(false)
    var consumption by mutableStateOf("")
    var pricePerLiter by mutableStateOf("")

    private var suggestedConsumption: String? by mutableStateOf(null)
    private var suggestedPrice: String? by mutableStateOf(null)

    val parsedDistance: Double? get() = NumberInput.double(distance, NumberInput.Style.KILOMETERS)
    val parsedConsumption: Double? get() = NumberInput.double(consumption, NumberInput.Style.CONSUMPTION)
    val parsedPrice: BigDecimal? get() = NumberInput.decimal(pricePerLiter, NumberInput.Style.FUEL_PRICE)

    val isUsingAverageConsumption: Boolean get() = suggestedConsumption != null && consumption == suggestedConsumption
    val isUsingLastPrice: Boolean get() = suggestedPrice != null && pricePerLiter == suggestedPrice

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
        try {
            val fuel = fuelEntryRepository.entries(vehicle.id)
            val average = Consumption.average(fuel)
            val lastPrice = fuel.sortedChronologically().lastOrNull()?.pricePerLiter
            applySuggestions(
                consumption = average?.let { NumberInput.text(it, NumberInput.Style.CONSUMPTION) },
                price = lastPrice?.let { NumberInput.text(it, NumberInput.Style.FUEL_PRICE) },
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            state = State.Failed(error)
        }
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
