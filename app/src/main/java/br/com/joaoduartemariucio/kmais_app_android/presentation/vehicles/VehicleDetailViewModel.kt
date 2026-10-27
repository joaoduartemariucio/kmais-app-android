package br.com.joaoduartemariucio.kmais_app_android.presentation.vehicles

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import br.com.joaoduartemariucio.kmais_app_android.domain.calculations.Odometer
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.FuelEntry
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.OdometerSample
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.ServiceEntry
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.ServiceRule
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.Vehicle
import br.com.joaoduartemariucio.kmais_app_android.domain.repositories.FuelEntryRepository
import br.com.joaoduartemariucio.kmais_app_android.domain.repositories.ServiceRepository
import br.com.joaoduartemariucio.kmais_app_android.domain.services.DateProvider
import br.com.joaoduartemariucio.kmais_app_android.presentation.reminders.ServiceReminderSync
import java.math.BigDecimal
import kotlin.coroutines.cancellation.CancellationException

class VehicleDetailViewModel(
    val vehicle: Vehicle,
    private val fuelEntryRepository: FuelEntryRepository,
    private val serviceRepository: ServiceRepository,
    private val dateProvider: DateProvider,
    private val reminders: ServiceReminderSync? = null,
) : ViewModel() {
    sealed interface State {
        data object Loading : State

        data class Loaded(
            val content: Content,
        ) : State

        data class Failed(
            val error: Throwable,
        ) : State
    }

    data class Content(
        val stats: VehicleStats,
        val sections: List<VehicleTimeline.MonthSection>,
        val spending: List<VehicleTimeline.MonthSpending>,
        val rules: List<ServiceRule>,
    )

    var state: State by mutableStateOf(State.Loading)
        private set

    var filter: VehicleTimeline.Filter by mutableStateOf(VehicleTimeline.Filter.ALL)

    private var fuel: List<FuelEntry> = emptyList()
    private var services: List<ServiceEntry> = emptyList()

    private val content: Content? get() = (state as? State.Loaded)?.content

    suspend fun load() {
        state =
            try {
                val fuel = fuelEntryRepository.entries(vehicle.id)
                val services = serviceRepository.entries(vehicle.id)
                val rules = serviceRepository.rules(vehicle.id)

                this.fuel = fuel
                this.services = services

                val zone = dateProvider.zone
                val now = dateProvider.now
                val sections = VehicleTimeline.sections(fuel, services, zone)

                State.Loaded(
                    Content(
                        stats = VehicleStats(vehicle, fuel, services, rules, now, zone),
                        sections = sections,
                        spending = VehicleTimeline.spending(sections, SPENDING_MONTHS, now, zone),
                        rules = rules,
                    ),
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                State.Failed(error)
            }
        reminders?.sync()
    }

    val visibleSections: List<VehicleTimeline.MonthSection>
        get() = content?.sections?.mapNotNull { it.filtered(filter) }.orEmpty()

    val hasHistory: Boolean get() = content?.sections?.isNotEmpty() == true

    val lastPricePerLiter: BigDecimal? get() = content?.stats?.lastPricePerLiter

    val rules: List<ServiceRule> get() = content?.rules.orEmpty()

    fun odometerReadings(excluding: VehicleTimeline.Item? = null): List<OdometerSample> =
        Odometer.samples(
            vehicle = vehicle,
            fuel = fuel.filter { it.id != excluding?.id },
            services = services.filter { it.id != excluding?.id },
        )

    suspend fun delete(item: VehicleTimeline.Item) {
        try {
            when (item) {
                is VehicleTimeline.Item.Fuel -> fuelEntryRepository.delete(item.entry.id)
                is VehicleTimeline.Item.Service -> serviceRepository.deleteEntry(item.entry.id)
            }
            load()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            state = State.Failed(error)
        }
    }

    companion object {
        const val SPENDING_MONTHS = 6
    }
}
