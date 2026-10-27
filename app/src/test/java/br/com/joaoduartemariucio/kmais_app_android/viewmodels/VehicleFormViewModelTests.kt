package br.com.joaoduartemariucio.kmais_app_android.viewmodels

import br.com.joaoduartemariucio.kmais_app_android.calculations.day
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.ServiceKind
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.ServiceRule
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.Vehicle
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.VehicleKind
import br.com.joaoduartemariucio.kmais_app_android.domain.errors.RepositoryError
import br.com.joaoduartemariucio.kmais_app_android.fakes.FixedDateProvider
import br.com.joaoduartemariucio.kmais_app_android.fakes.InMemoryServiceRepository
import br.com.joaoduartemariucio.kmais_app_android.fakes.InMemoryVehicleRepository
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.FormState
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.NumberInput
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.ValidationIssue
import br.com.joaoduartemariucio.kmais_app_android.presentation.vehicles.VehicleFormViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VehicleFormViewModelTests {
    private fun makeViewModel(
        mode: VehicleFormViewModel.Mode = VehicleFormViewModel.Mode.Create,
        existingRules: List<ServiceRule> = emptyList(),
        vehicleRepository: InMemoryVehicleRepository = InMemoryVehicleRepository(),
        serviceRepository: InMemoryServiceRepository = InMemoryServiceRepository(),
    ) = VehicleFormViewModel(mode, existingRules, vehicleRepository, serviceRepository, FixedDateProvider(day(30)))

    @Test
    fun `a new vehicle needs a name`() {
        val viewModel = makeViewModel()
        viewModel.name = "   "

        assertEquals(listOf(ValidationIssue.NameRequired), viewModel.issues)
        assertFalse(viewModel.canSave)
    }

    @Test
    fun `a reminder needs a distance or a time interval`() {
        val viewModel = makeViewModel()
        viewModel.name = "Gol"
        viewModel.addRule()

        assertEquals(listOf(ValidationIssue.RuleWithoutInterval), viewModel.issues)

        viewModel.rules[0].monthInterval = NumberInput.text(BigDecimal(6), NumberInput.Style.MONTHS)
        assertTrue(viewModel.issues.isEmpty())
    }

    @Test
    fun `saving a new vehicle stores it with its reminders`() =
        runTest {
            val vehicles = InMemoryVehicleRepository()
            val services = InMemoryServiceRepository()
            val viewModel = makeViewModel(vehicleRepository = vehicles, serviceRepository = services)
            viewModel.name = "  Gol  "
            viewModel.kind = VehicleKind.MOTORCYCLE
            viewModel.plate = " abc1d23 "
            viewModel.initialOdometer = NumberInput.text(56_000.0, NumberInput.Style.KILOMETERS)
            viewModel.tankCapacity = NumberInput.text(50.0, NumberInput.Style.LITERS)
            viewModel.addRule()
            viewModel.rules[0].kilometerInterval = NumberInput.text(10_000.0, NumberInput.Style.KILOMETERS)

            viewModel.save()

            val saved = assertNotNull(vehicles.all(includingArchived = true).firstOrNull())
            assertEquals(FormState.Saved, viewModel.state)
            assertEquals("Gol", saved.name)
            assertEquals(VehicleKind.MOTORCYCLE, saved.kind)
            assertEquals("ABC1D23", saved.plate)
            assertEquals(56_000.0, saved.initialOdometer)
            assertEquals(50.0, saved.tankCapacity)
            assertEquals(day(30), saved.createdAt)

            val rules = services.rules(saved.id)
            assertEquals(1, rules.size)
            assertEquals(10_000.0, rules.first().kilometerInterval)
            assertNull(rules.first().monthInterval)
        }

    @Test
    fun `editing pre-fills the vehicle and its reminders, sorted by kind`() =
        runTest {
            val vehicle =
                Vehicle(
                    name = "Jetta",
                    plate = "XYZ9A87",
                    initialOdometer = 144_000.0,
                    tankCapacity = 55.0,
                    createdAt = day(0),
                    isArchived = true,
                )
            val rules =
                listOf(
                    ServiceRule(vehicleId = vehicle.id, kind = ServiceKind.TIRES, monthInterval = 12),
                    ServiceRule(vehicleId = vehicle.id, kind = ServiceKind.BRAKES, kilometerInterval = 20_000.0),
                )
            val vehicles = InMemoryVehicleRepository(listOf(vehicle))
            val viewModel = makeViewModel(VehicleFormViewModel.Mode.Edit(vehicle), rules, vehicles)

            assertEquals("Jetta", viewModel.name)
            assertEquals("XYZ9A87", viewModel.plate)
            assertEquals(144_000.0, viewModel.parsedInitialOdometer)
            assertEquals(55.0, viewModel.parsedTankCapacity)
            assertEquals(listOf(ServiceKind.BRAKES, ServiceKind.TIRES), viewModel.rules.map { it.kind })
            assertEquals(12, viewModel.rules[1].parsedMonthInterval)

            viewModel.plate = ""
            viewModel.save()

            val saved = assertNotNull(vehicles.vehicle(vehicle.id))
            assertNull(saved.plate)
            assertEquals(day(0), saved.createdAt)
            assertTrue(saved.isArchived)
        }

    @Test
    fun `removing reminders ignores indices out of range`() {
        val viewModel = makeViewModel()
        viewModel.addRule()
        viewModel.addRule()
        val kept = viewModel.rules[1].id

        viewModel.removeRules(setOf(0, 5))

        assertEquals(listOf(kept), viewModel.rules.map { it.id })
    }

    @Test
    fun `a blocking issue prevents saving`() =
        runTest {
            val vehicles = InMemoryVehicleRepository()
            val viewModel = makeViewModel(vehicleRepository = vehicles)

            viewModel.save()

            assertEquals(FormState.Editing, viewModel.state)
            assertTrue(vehicles.all(includingArchived = true).isEmpty())
        }

    @Test
    fun `a repository failure is shown to the user`() =
        runTest {
            val viewModel = makeViewModel(vehicleRepository = InMemoryVehicleRepository(failure = RepositoryError.VehicleNotFound(UUID.randomUUID())))
            viewModel.name = "Gol"

            viewModel.save()

            assertIs<FormState.Failed>(viewModel.state)
        }
}
