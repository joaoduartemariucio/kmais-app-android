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
import br.com.joaoduartemariucio.rodaro.domain.maintenance.entities.ServiceEntry
import br.com.joaoduartemariucio.rodaro.domain.maintenance.entities.ServiceKind
import br.com.joaoduartemariucio.rodaro.domain.vehicles.entities.OdometerSample
import br.com.joaoduartemariucio.rodaro.fakes.FixedDateProvider
import br.com.joaoduartemariucio.rodaro.fakes.InMemoryServiceRepository
import br.com.joaoduartemariucio.rodaro.presentation.common.FormState
import br.com.joaoduartemariucio.rodaro.presentation.common.NumberInput
import br.com.joaoduartemariucio.rodaro.presentation.common.ValidationIssue
import br.com.joaoduartemariucio.rodaro.presentation.maintenance.form.ServiceEntryFormViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ServiceEntryFormViewModelTests {
    private fun makeViewModel(
        mode: ServiceEntryFormViewModel.Mode = ServiceEntryFormViewModel.Mode.Create(testVehicleId),
        repository: InMemoryServiceRepository = InMemoryServiceRepository(),
        otherReadings: List<OdometerSample> = emptyList(),
    ) = ServiceEntryFormViewModel(mode, repository, FixedDateProvider(day(30)), otherReadings)

    @Test
    fun `a new form defaults to an oil change today and only needs the odometer`() {
        val viewModel = makeViewModel()

        assertEquals(ServiceKind.OIL_CHANGE, viewModel.kind)
        assertEquals(day(30), viewModel.date)
        assertEquals(listOf(ValidationIssue.OdometerRequired), viewModel.issues)
        assertFalse(viewModel.canSave)
    }

    @Test
    fun `an empty cost is saved as zero and notes are trimmed`() =
        runTest {
            val repository = InMemoryServiceRepository()
            val viewModel = makeViewModel(repository = repository)
            viewModel.kind = ServiceKind.TIRES
            viewModel.odometer = NumberInput.text(1_500.0, NumberInput.Style.KILOMETERS)
            viewModel.notes = "  front tires  \n"

            assertTrue(viewModel.canSave)
            viewModel.save()

            val saved = repository.entries(testVehicleId)
            assertEquals(FormState.Saved, viewModel.state)
            assertEquals(1, saved.size)
            assertEquals(ServiceKind.TIRES, saved.first().kind)
            assertEquals(1_500.0, saved.first().odometer)
            assertDecimal("0", saved.first().cost)
            assertEquals("front tires", saved.first().notes)
        }

    @Test
    fun `editing pre-fills the fields and keeps the entry's identity`() =
        runTest {
            val existing =
                ServiceEntry(
                    vehicleId = testVehicleId,
                    kind = ServiceKind.BRAKES,
                    date = day(10),
                    odometer = 1_200.0,
                    cost = BigDecimal(350),
                    notes = "pads",
                )
            val repository = InMemoryServiceRepository(listOf(existing))
            val viewModel = makeViewModel(ServiceEntryFormViewModel.Mode.Edit(existing), repository)

            assertEquals(ServiceKind.BRAKES, viewModel.kind)
            assertEquals(1_200.0, viewModel.parsedOdometer)
            assertDecimal("350", viewModel.parsedCost)
            assertEquals("pads", viewModel.notes)

            viewModel.cost = NumberInput.text(BigDecimal(400), NumberInput.Style.MONEY)
            viewModel.save()

            val saved = repository.entries(testVehicleId)
            assertEquals(listOf(existing.id), saved.map { it.id })
            assertDecimal("400", saved.first().cost)
        }

    @Test
    fun `an odometer below the previous record warns but still saves`() =
        runTest {
            val viewModel = makeViewModel(otherReadings = listOf(sample(10, 2_000.0)))
            viewModel.odometer = NumberInput.text(1_900.0, NumberInput.Style.KILOMETERS)

            assertEquals(listOf(ValidationIssue.OdometerNotAfterPrevious(2_000.0)), viewModel.issues)

            viewModel.save()
            assertEquals(FormState.Saved, viewModel.state)
        }

    @Test
    fun `a repository failure is shown to the user`() =
        runTest {
            val viewModel = makeViewModel(repository = InMemoryServiceRepository(failure = RepositoryError.VehicleNotFound(testVehicleId)))
            viewModel.odometer = NumberInput.text(1_500.0, NumberInput.Style.KILOMETERS)

            viewModel.save()

            assertIs<FormState.Failed>(viewModel.state)
        }

    @Test
    fun `the form shows the reading right before its date`() {
        assertNull(makeViewModel().previousReading)
        assertEquals(1_000.0, makeViewModel(otherReadings = listOf(sample(10, 1_000.0), sample(60, 2_000.0))).previousReading)
    }
}
