package br.com.joaoduartemariucio.kmais_app_android.presentation.vehicles

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.ServiceKind
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.ServiceRule
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.Vehicle
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.VehicleKind
import br.com.joaoduartemariucio.kmais_app_android.domain.repositories.ServiceRepository
import br.com.joaoduartemariucio.kmais_app_android.domain.repositories.VehicleRepository
import br.com.joaoduartemariucio.kmais_app_android.domain.services.DateProvider
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.FormState
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.NumberInput
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.ValidationIssue
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.blocksSaving
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

class VehicleFormViewModel(
    private val mode: Mode,
    existingRules: List<ServiceRule> = emptyList(),
    private val vehicleRepository: VehicleRepository,
    private val serviceRepository: ServiceRepository,
    private val dateProvider: DateProvider,
) : ViewModel() {
    class RuleDraft(
        val id: UUID = UUID.randomUUID(),
        kind: ServiceKind = ServiceKind.OIL_CHANGE,
        kilometerInterval: String = "",
        monthInterval: String = "",
    ) {
        var kind by mutableStateOf(kind)
        var kilometerInterval by mutableStateOf(kilometerInterval)
        var monthInterval by mutableStateOf(monthInterval)

        constructor(rule: ServiceRule) : this(
            id = rule.id,
            kind = rule.kind,
            kilometerInterval = NumberInput.text(rule.kilometerInterval, NumberInput.Style.KILOMETERS),
            monthInterval = NumberInput.text(rule.monthInterval?.toBigDecimal(), NumberInput.Style.MONTHS),
        )

        val parsedKilometerInterval: Double?
            get() = NumberInput.double(kilometerInterval, NumberInput.Style.KILOMETERS)?.takeIf { it > 0 }

        val parsedMonthInterval: Int?
            get() = NumberInput.double(monthInterval, NumberInput.Style.MONTHS)?.takeIf { it > 0 }?.toInt()

        val issues: List<ValidationIssue>
            get() =
                if (parsedKilometerInterval == null && parsedMonthInterval == null) {
                    listOf(ValidationIssue.RuleWithoutInterval)
                } else {
                    emptyList()
                }
    }

    sealed interface Mode {
        data object Create : Mode

        data class Edit(
            val vehicle: Vehicle,
        ) : Mode
    }

    var name by mutableStateOf("")
    var kind by mutableStateOf(VehicleKind.CAR)
    var plate by mutableStateOf("")
    var initialOdometer by mutableStateOf("")
    var tankCapacity by mutableStateOf("")
    val rules = mutableStateListOf<RuleDraft>()

    var state: FormState by mutableStateOf(FormState.Editing)
        private set

    init {
        if (mode is Mode.Edit) {
            val vehicle = mode.vehicle
            name = vehicle.name
            kind = vehicle.kind
            plate = vehicle.plate.orEmpty()
            initialOdometer = NumberInput.text(vehicle.initialOdometer, NumberInput.Style.KILOMETERS)
            tankCapacity = NumberInput.text(vehicle.tankCapacity, NumberInput.Style.LITERS)
        }
        rules.addAll(existingRules.sortedBy { it.kind.rawValue }.map(::RuleDraft))
    }

    val trimmedName: String get() = name.trim()

    val parsedPlate: String? get() = plate.trim().takeIf { it.isNotEmpty() }?.uppercase()

    val parsedInitialOdometer: Double get() = NumberInput.double(initialOdometer, NumberInput.Style.KILOMETERS) ?: 0.0

    val parsedTankCapacity: Double? get() = NumberInput.double(tankCapacity, NumberInput.Style.LITERS)

    val issues: List<ValidationIssue>
        get() =
            buildList {
                if (trimmedName.isEmpty()) add(ValidationIssue.NameRequired)
                addAll(rules.flatMap { it.issues })
            }

    val canSave: Boolean get() = !issues.blocksSaving && !state.isSaving

    fun addRule() {
        rules += RuleDraft()
    }

    fun removeRules(offsets: Set<Int>) {
        offsets.sortedDescending().filter { it in rules.indices }.forEach { rules.removeAt(it) }
    }

    suspend fun save() {
        if (issues.blocksSaving) return

        state = FormState.Saving
        val vehicle = vehicle()
        state =
            try {
                vehicleRepository.save(vehicle)
                serviceRepository.save(
                    rules.map { draft ->
                        ServiceRule(
                            id = draft.id,
                            vehicleId = vehicle.id,
                            kind = draft.kind,
                            kilometerInterval = draft.parsedKilometerInterval,
                            monthInterval = draft.parsedMonthInterval,
                        )
                    },
                    vehicle.id,
                )
                FormState.Saved
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                FormState.Failed(error)
            }
    }

    private fun vehicle(): Vehicle =
        when (mode) {
            Mode.Create -> {
                Vehicle(
                    name = trimmedName,
                    kind = kind,
                    plate = parsedPlate,
                    initialOdometer = parsedInitialOdometer,
                    tankCapacity = parsedTankCapacity,
                    createdAt = dateProvider.now,
                )
            }

            is Mode.Edit -> {
                mode.vehicle.copy(
                    name = trimmedName,
                    kind = kind,
                    plate = parsedPlate,
                    initialOdometer = parsedInitialOdometer,
                    tankCapacity = parsedTankCapacity,
                )
            }
        }
}
