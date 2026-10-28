package br.com.joaoduartemariucio.rodaro.viewmodels

import br.com.joaoduartemariucio.rodaro.calculations.assertDecimal
import br.com.joaoduartemariucio.rodaro.calculations.day
import br.com.joaoduartemariucio.rodaro.calculations.decimal
import br.com.joaoduartemariucio.rodaro.calculations.full
import br.com.joaoduartemariucio.rodaro.calculations.partial
import br.com.joaoduartemariucio.rodaro.calculations.rule
import br.com.joaoduartemariucio.rodaro.calculations.sample
import br.com.joaoduartemariucio.rodaro.calculations.service
import br.com.joaoduartemariucio.rodaro.calculations.testVehicleId
import br.com.joaoduartemariucio.rodaro.calculations.utc
import br.com.joaoduartemariucio.rodaro.domain.common.errors.RepositoryError
import br.com.joaoduartemariucio.rodaro.domain.vehicles.entities.OdometerSample
import br.com.joaoduartemariucio.rodaro.fakes.FixedDateProvider
import br.com.joaoduartemariucio.rodaro.fakes.InMemoryFuelEntryRepository
import br.com.joaoduartemariucio.rodaro.presentation.common.FormState
import br.com.joaoduartemariucio.rodaro.presentation.common.NumberInput
import br.com.joaoduartemariucio.rodaro.presentation.common.ValidationIssue
import br.com.joaoduartemariucio.rodaro.presentation.fueling.form.FuelEntryFormViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FuelEntryFormViewModelTests {
    private fun makeViewModel(
        mode: FuelEntryFormViewModel.Mode = FuelEntryFormViewModel.Mode.Create(testVehicleId),
        repository: InMemoryFuelEntryRepository = InMemoryFuelEntryRepository(),
        otherReadings: List<OdometerSample> = emptyList(),
        suggestedPrice: BigDecimal? = null,
    ) = FuelEntryFormViewModel(mode, repository, FixedDateProvider(day(30)), otherReadings, suggestedPrice)

    private fun FuelEntryFormViewModel.fill(
        odometer: Double = 1_500.0,
        liters: Double = 40.0,
        price: BigDecimal = BigDecimal(5),
    ) {
        this.odometer = NumberInput.text(odometer, NumberInput.Style.KILOMETERS)
        this.liters = NumberInput.text(liters, NumberInput.Style.LITERS)
        pricePerLiter = NumberInput.text(price, NumberInput.Style.FUEL_PRICE)
    }

    @Test
    fun `a new form starts today, full tank, with every field required`() {
        val viewModel = makeViewModel()

        assertEquals(day(30), viewModel.date)
        assertTrue(viewModel.isFullTank)
        assertEquals(
            listOf(ValidationIssue.OdometerRequired, ValidationIssue.LitersRequired, ValidationIssue.PriceRequired),
            viewModel.issues,
        )
        assertFalse(viewModel.canSave)
        assertNull(viewModel.totalCost)
    }

    @Test
    fun `zero liters and zero price are rejected`() {
        val viewModel = makeViewModel()
        viewModel.fill(liters = 0.0, price = BigDecimal.ZERO)

        assertEquals(listOf(ValidationIssue.LitersNotPositive, ValidationIssue.PriceNotPositive), viewModel.issues)
        assertFalse(viewModel.canSave)
    }

    @Test
    fun `the total is liters times price`() {
        val viewModel = makeViewModel()
        viewModel.fill(liters = 40.0, price = BigDecimal(5))

        assertDecimal("200", viewModel.totalCost)
    }

    @Test
    fun `saving creates the entry for the vehicle`() =
        runTest {
            val repository = InMemoryFuelEntryRepository()
            val viewModel = makeViewModel(repository = repository)
            viewModel.fill()
            viewModel.isFullTank = false

            viewModel.save()

            val saved = repository.entries(testVehicleId)
            assertEquals(FormState.Saved, viewModel.state)
            assertEquals(1, saved.size)
            assertEquals(1_500.0, saved.first().odometer)
            assertEquals(40.0, saved.first().liters)
            assertDecimal("5", saved.first().pricePerLiter)
            assertFalse(saved.first().isFullTank)
        }

    @Test
    fun `editing pre-fills the fields and keeps the entry's identity`() =
        runTest {
            val existing = full(10, 1_200.0, 35.0, price = BigDecimal(6))
            val repository = InMemoryFuelEntryRepository(listOf(existing))
            val viewModel = makeViewModel(FuelEntryFormViewModel.Mode.Edit(existing), repository)

            assertEquals(1_200.0, viewModel.parsedOdometer)
            assertEquals(35.0, viewModel.parsedLiters)
            assertDecimal("6", viewModel.parsedPrice)
            assertEquals(day(10), viewModel.date)

            viewModel.liters = NumberInput.text(36.0, NumberInput.Style.LITERS)
            viewModel.save()

            val saved = repository.entries(testVehicleId)
            assertEquals(listOf(existing.id), saved.map { it.id })
            assertEquals(36.0, saved.first().liters)
        }

    @Test
    fun `an odometer out of order warns but still saves`() =
        runTest {
            val viewModel = makeViewModel(otherReadings = listOf(sample(40, 2_000.0)))
            viewModel.fill(odometer = 2_100.0)

            assertEquals(listOf(ValidationIssue.OdometerNotBeforeNext(2_000.0)), viewModel.issues)
            assertTrue(viewModel.canSave)

            viewModel.save()
            assertEquals(FormState.Saved, viewModel.state)
        }

    @Test
    fun `a blocking issue prevents saving`() =
        runTest {
            val repository = InMemoryFuelEntryRepository()
            val viewModel = makeViewModel(repository = repository)

            viewModel.save()

            assertEquals(FormState.Editing, viewModel.state)
            assertTrue(repository.entries(testVehicleId).isEmpty())
        }

    @Test
    fun `a repository failure is shown to the user`() =
        runTest {
            val viewModel = makeViewModel(repository = InMemoryFuelEntryRepository(failure = RepositoryError.VehicleNotFound(testVehicleId)))
            viewModel.fill()

            viewModel.save()

            assertIs<FormState.Failed>(viewModel.state)
        }

    @Test
    fun `a new fuel-up starts with the suggested price - an edit keeps its own`() {
        val created = makeViewModel(suggestedPrice = decimal("5.899"))
        assertDecimal("5.899", created.parsedPrice)
        assertTrue(created.isUsingSuggestedPrice)
        created.pricePerLiter = NumberInput.text(BigDecimal(6), NumberInput.Style.FUEL_PRICE)
        assertFalse(created.isUsingSuggestedPrice)

        val edited = makeViewModel(FuelEntryFormViewModel.Mode.Edit(full(10, 1_000.0, 30.0, price = BigDecimal(6))), suggestedPrice = BigDecimal(4))
        assertDecimal("6", edited.parsedPrice)
        assertFalse(edited.isUsingSuggestedPrice)
        assertFalse(makeViewModel().isUsingSuggestedPrice)
    }

    @Test
    fun `the form shows the previous reading and the distance driven since`() {
        val viewModel = makeViewModel(otherReadings = listOf(sample(10, 1_000.0), sample(60, 2_000.0)))

        assertEquals(1_000.0, viewModel.previousReading)
        assertNull(viewModel.distanceSincePrevious)

        viewModel.odometer = NumberInput.text(1_350.0, NumberInput.Style.KILOMETERS)
        assertEquals(350.0, viewModel.distanceSincePrevious)

        viewModel.odometer = NumberInput.text(900.0, NumberInput.Style.KILOMETERS)
        assertNull(viewModel.distanceSincePrevious)
    }
}
