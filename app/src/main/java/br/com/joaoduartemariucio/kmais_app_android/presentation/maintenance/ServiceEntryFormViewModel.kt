package br.com.joaoduartemariucio.kmais_app_android.presentation.maintenance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import br.com.joaoduartemariucio.kmais_app_android.domain.calculations.Odometer
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.OdometerSample
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.ServiceEntry
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.ServiceKind
import br.com.joaoduartemariucio.kmais_app_android.domain.repositories.ServiceRepository
import br.com.joaoduartemariucio.kmais_app_android.domain.services.DateProvider
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.FormState
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.NumberInput
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.ValidationIssue
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.blocksSaving
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

class ServiceEntryFormViewModel(
    private val mode: Mode,
    private val repository: ServiceRepository,
    dateProvider: DateProvider,
    private val otherReadings: List<OdometerSample>,
) : ViewModel() {
    sealed interface Mode {
        data class Create(
            val vehicleId: UUID,
        ) : Mode

        data class Edit(
            val entry: ServiceEntry,
        ) : Mode
    }

    var kind by mutableStateOf(if (mode is Mode.Edit) mode.entry.kind else ServiceKind.OIL_CHANGE)
    var date: Instant by mutableStateOf(if (mode is Mode.Edit) mode.entry.date else dateProvider.now)
    var odometer by mutableStateOf(if (mode is Mode.Edit) NumberInput.text(mode.entry.odometer, NumberInput.Style.KILOMETERS) else "")
    var cost by mutableStateOf(if (mode is Mode.Edit) NumberInput.text(mode.entry.cost, NumberInput.Style.MONEY) else "")
    var notes by mutableStateOf(if (mode is Mode.Edit) mode.entry.notes else "")

    var state: FormState by mutableStateOf(FormState.Editing)
        private set

    val parsedOdometer: Double? get() = NumberInput.double(odometer, NumberInput.Style.KILOMETERS)

    val previousReading: Double? get() = Odometer.neighbors(date, otherReadings).previous

    val parsedCost: BigDecimal get() = NumberInput.decimal(cost, NumberInput.Style.MONEY) ?: BigDecimal.ZERO

    val issues: List<ValidationIssue>
        get() =
            buildList {
                addAll(ValidationIssue.odometer(parsedOdometer, date, otherReadings))
                if (parsedCost.signum() < 0) add(ValidationIssue.CostNegative)
            }

    val canSave: Boolean get() = !issues.blocksSaving && !state.isSaving

    suspend fun save() {
        val odometer = parsedOdometer ?: return
        if (issues.blocksSaving) return

        state = FormState.Saving
        state =
            try {
                repository.save(entry(odometer))
                FormState.Saved
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                FormState.Failed(error)
            }
    }

    private fun entry(odometer: Double): ServiceEntry {
        val trimmedNotes = notes.trim()
        return when (mode) {
            is Mode.Create -> {
                ServiceEntry(
                    vehicleId = mode.vehicleId,
                    kind = kind,
                    date = date,
                    odometer = odometer,
                    cost = parsedCost,
                    notes = trimmedNotes,
                )
            }

            is Mode.Edit -> {
                mode.entry.copy(kind = kind, date = date, odometer = odometer, cost = parsedCost, notes = trimmedNotes)
            }
        }
    }
}
