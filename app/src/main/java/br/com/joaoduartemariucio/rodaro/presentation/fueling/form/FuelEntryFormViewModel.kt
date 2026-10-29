package br.com.joaoduartemariucio.rodaro.presentation.fueling.form

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import br.com.joaoduartemariucio.rodaro.app.NoAnalytics
import br.com.joaoduartemariucio.rodaro.domain.common.services.Analytics
import br.com.joaoduartemariucio.rodaro.domain.common.services.DateProvider
import br.com.joaoduartemariucio.rodaro.domain.fueling.entities.FuelEntry
import br.com.joaoduartemariucio.rodaro.domain.fueling.repositories.FuelEntryRepository
import br.com.joaoduartemariucio.rodaro.domain.vehicles.calculations.Odometer
import br.com.joaoduartemariucio.rodaro.domain.vehicles.entities.OdometerSample
import br.com.joaoduartemariucio.rodaro.presentation.common.FormState
import br.com.joaoduartemariucio.rodaro.presentation.common.NumberInput
import br.com.joaoduartemariucio.rodaro.presentation.common.UnitConversion
import br.com.joaoduartemariucio.rodaro.presentation.common.ValidationIssue
import br.com.joaoduartemariucio.rodaro.presentation.common.blocksSaving
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

class FuelEntryFormViewModel(
    private val mode: Mode,
    private val repository: FuelEntryRepository,
    dateProvider: DateProvider,
    private val otherReadings: List<OdometerSample>,
    suggestedPrice: BigDecimal? = null,
    private val analytics: Analytics = NoAnalytics,
) : ViewModel() {
    sealed interface Mode {
        data class Create(
            val vehicleId: UUID,
        ) : Mode

        data class Edit(
            val entry: FuelEntry,
        ) : Mode
    }

    private val suggestedPriceText: String? =
        if (mode is Mode.Create && suggestedPrice != null) {
            NumberInput.text(UnitConversion.perVolume(suggestedPrice), NumberInput.Style.FUEL_PRICE)
        } else {
            null
        }

    var date: Instant by mutableStateOf(if (mode is Mode.Edit) mode.entry.date else dateProvider.now)
    var odometer by mutableStateOf(
        if (mode is Mode.Edit) NumberInput.text(UnitConversion.distance(mode.entry.odometer), NumberInput.Style.KILOMETERS) else "",
    )
    var liters by mutableStateOf(
        if (mode is Mode.Edit) NumberInput.text(UnitConversion.volume(mode.entry.liters), NumberInput.Style.LITERS) else "",
    )
    var pricePerLiter by mutableStateOf(
        if (mode is Mode.Edit) {
            NumberInput.text(UnitConversion.perVolume(mode.entry.pricePerLiter), NumberInput.Style.FUEL_PRICE)
        } else {
            suggestedPriceText.orEmpty()
        },
    )
    var isFullTank by mutableStateOf(if (mode is Mode.Edit) mode.entry.isFullTank else true)

    var state: FormState by mutableStateOf(FormState.Editing)
        private set

    val parsedOdometer: Double?
        get() = NumberInput.double(odometer, NumberInput.Style.KILOMETERS)?.let(UnitConversion::distanceToKilometers)

    val parsedLiters: Double?
        get() = NumberInput.double(liters, NumberInput.Style.LITERS)?.let(UnitConversion::volumeToLiters)

    val parsedPrice: BigDecimal?
        get() = NumberInput.decimal(pricePerLiter, NumberInput.Style.FUEL_PRICE)?.let(UnitConversion::perVolumeToLiters)

    val isUsingSuggestedPrice: Boolean get() = suggestedPriceText != null && pricePerLiter == suggestedPriceText

    val previousReading: Double? get() = Odometer.neighbors(date, otherReadings).previous

    val distanceSincePrevious: Double?
        get() {
            val odometer = parsedOdometer ?: return null
            val previous = previousReading ?: return null
            return if (odometer > previous) odometer - previous else null
        }

    val totalCost: BigDecimal?
        get() {
            val liters = parsedLiters ?: return null
            val price = parsedPrice ?: return null
            return entry(parsedOdometer ?: 0.0, liters, price).totalCost
        }

    val issues: List<ValidationIssue>
        get() =
            buildList {
                addAll(ValidationIssue.odometer(parsedOdometer, date, otherReadings))

                val liters = parsedLiters
                if (liters == null) {
                    add(ValidationIssue.LitersRequired)
                } else if (liters <= 0) {
                    add(ValidationIssue.LitersNotPositive)
                }

                val price = parsedPrice
                if (price == null) {
                    add(ValidationIssue.PriceRequired)
                } else if (price.signum() <= 0) {
                    add(ValidationIssue.PriceNotPositive)
                }
            }

    val canSave: Boolean get() = !issues.blocksSaving && !state.isSaving

    suspend fun save() {
        val odometer = parsedOdometer ?: return
        val liters = parsedLiters ?: return
        val price = parsedPrice ?: return
        if (issues.blocksSaving) return

        state = FormState.Saving
        state =
            try {
                repository.save(entry(odometer, liters, price))
                analytics.event(
                    "fuel_up_saved",
                    mapOf(
                        "mode" to if (mode is Mode.Create) "create" else "edit",
                        "full_tank" to isFullTank.toString(),
                        "distance_since_previous" to (distanceSincePrevious?.toString() ?: "unknown"),
                    ),
                )
                FormState.Saved
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                FormState.Failed(error)
            }
    }

    private fun entry(
        odometer: Double,
        liters: Double,
        price: BigDecimal,
    ): FuelEntry =
        when (mode) {
            is Mode.Create -> {
                FuelEntry(
                    vehicleId = mode.vehicleId,
                    date = date,
                    odometer = odometer,
                    liters = liters,
                    pricePerLiter = price,
                    isFullTank = isFullTank,
                )
            }

            is Mode.Edit -> {
                mode.entry.copy(
                    date = date,
                    odometer = odometer,
                    liters = liters,
                    pricePerLiter = price,
                    isFullTank = isFullTank,
                )
            }
        }
}
