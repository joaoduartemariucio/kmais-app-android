package br.com.joaoduartemariucio.kmais_app_android.presentation.vehicles

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import br.com.joaoduartemariucio.kmais_app_android.domain.calculations.MaintenanceStatus
import br.com.joaoduartemariucio.kmais_app_android.domain.calculations.sum
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.Vehicle
import br.com.joaoduartemariucio.kmais_app_android.domain.repositories.FuelEntryRepository
import br.com.joaoduartemariucio.kmais_app_android.domain.repositories.ServiceRepository
import br.com.joaoduartemariucio.kmais_app_android.domain.repositories.VehicleRepository
import br.com.joaoduartemariucio.kmais_app_android.domain.services.DateProvider
import br.com.joaoduartemariucio.kmais_app_android.presentation.reminders.ServiceReminderSync
import java.math.BigDecimal
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

class VehicleListViewModel(
    val scope: Scope,
    private val vehicleRepository: VehicleRepository,
    private val fuelEntryRepository: FuelEntryRepository,
    private val serviceRepository: ServiceRepository,
    private val dateProvider: DateProvider,
    private val reminders: ServiceReminderSync? = null,
) : ViewModel() {
    enum class Scope {
        ACTIVE,
        ARCHIVED,
    }

    sealed interface State {
        data object Loading : State

        data class Loaded(
            val rows: List<Row>,
        ) : State

        data class Failed(
            val error: Throwable,
        ) : State
    }

    data class Row(
        val vehicle: Vehicle,
        val stats: VehicleStats,
    ) {
        val id: UUID get() = vehicle.id
    }

    data class Overview(
        val vehicleCount: Int,
        val spentThisMonth: BigDecimal,
        val overdueCount: Int,
        val dueSoonCount: Int,
    )

    var state: State by mutableStateOf(State.Loading)
        private set

    val overview: Overview?
        get() {
            val rows = (state as? State.Loaded)?.rows?.takeIf { it.isNotEmpty() } ?: return null
            val levels = rows.flatMap { row -> row.stats.dues.map { it.status.level } }
            return Overview(
                vehicleCount = rows.size,
                spentThisMonth = rows.map { it.stats.spentThisMonth }.sum(),
                overdueCount = levels.count { it == MaintenanceStatus.Level.OVERDUE },
                dueSoonCount = levels.count { it == MaintenanceStatus.Level.DUE_SOON },
            )
        }

    suspend fun load() {
        state =
            try {
                val vehicles =
                    vehicleRepository
                        .all(includingArchived = scope == Scope.ARCHIVED)
                        .filter { if (scope == Scope.ARCHIVED) it.isArchived else !it.isArchived }

                State.Loaded(
                    vehicles.map { vehicle ->
                        Row(
                            vehicle = vehicle,
                            stats =
                                VehicleStats(
                                    vehicle = vehicle,
                                    fuel = fuelEntryRepository.entries(vehicle.id),
                                    services = serviceRepository.entries(vehicle.id),
                                    rules = serviceRepository.rules(vehicle.id),
                                    now = dateProvider.now,
                                    zone = dateProvider.zone,
                                ),
                        )
                    },
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                State.Failed(error)
            }
        reminders?.sync()
    }

    suspend fun setArchived(
        isArchived: Boolean,
        vehicle: Vehicle,
    ) {
        try {
            vehicleRepository.setArchived(isArchived, vehicle.id)
            load()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            state = State.Failed(error)
        }
    }
}
