package br.com.joaoduartemariucio.rodaro.viewmodels

import br.com.joaoduartemariucio.rodaro.calculations.assertDecimal
import br.com.joaoduartemariucio.rodaro.calculations.day
import br.com.joaoduartemariucio.rodaro.domain.common.calculations.sum
import br.com.joaoduartemariucio.rodaro.domain.common.errors.RepositoryError
import br.com.joaoduartemariucio.rodaro.domain.fueling.entities.FuelEntry
import br.com.joaoduartemariucio.rodaro.domain.maintenance.entities.ServiceEntry
import br.com.joaoduartemariucio.rodaro.domain.maintenance.entities.ServiceKind
import br.com.joaoduartemariucio.rodaro.domain.maintenance.entities.ServiceRule
import br.com.joaoduartemariucio.rodaro.domain.vehicles.calculations.Odometer
import br.com.joaoduartemariucio.rodaro.domain.vehicles.entities.Vehicle
import br.com.joaoduartemariucio.rodaro.fakes.FixedDateProvider
import br.com.joaoduartemariucio.rodaro.fakes.InMemoryFuelEntryRepository
import br.com.joaoduartemariucio.rodaro.fakes.InMemoryServiceRepository
import br.com.joaoduartemariucio.rodaro.presentation.vehicles.detail.VehicleDetailViewModel
import br.com.joaoduartemariucio.rodaro.presentation.vehicles.detail.VehicleDetailViewModel.State
import br.com.joaoduartemariucio.rodaro.presentation.vehicles.detail.VehicleTimeline
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

class VehicleDetailViewModelTests {
    private val vehicle = Vehicle(name = "Gol", initialOdometer = 1_000.0, createdAt = day(0))

    private fun fuel(
        dayOffset: Int,
        odometer: Double,
    ) = FuelEntry(
        vehicleId = vehicle.id,
        date = day(dayOffset),
        odometer = odometer,
        liters = 40.0,
        pricePerLiter = BigDecimal(5),
        isFullTank = true,
    )

    private fun makeViewModel(
        fuel: List<FuelEntry> = emptyList(),
        services: List<ServiceEntry> = emptyList(),
        rules: List<ServiceRule> = emptyList(),
        fuelFailure: Exception? = null,
    ) = VehicleDetailViewModel(
        vehicle = vehicle,
        fuelEntryRepository = InMemoryFuelEntryRepository(fuel, fuelFailure),
        serviceRepository = InMemoryServiceRepository(services, rules),
        dateProvider = FixedDateProvider(day(120)),
    )

    private val VehicleDetailViewModel.content get() = assertIs<State.Loaded>(state).content

    @Test
    fun `loads stats, timeline and rules together`() =
        runTest {
            val viewModel =
                makeViewModel(
                    fuel = listOf(fuel(1, 1_000.0), fuel(40, 1_400.0)),
                    services =
                        listOf(
                            ServiceEntry(vehicleId = vehicle.id, kind = ServiceKind.OIL_CHANGE, date = day(20), odometer = 1_200.0, cost = BigDecimal(300)),
                        ),
                    rules = listOf(ServiceRule(vehicleId = vehicle.id, kind = ServiceKind.OIL_CHANGE, kilometerInterval = 10_000.0, monthInterval = 12)),
                )

            viewModel.load()

            val content = viewModel.content
            assertEquals(1_400.0, content.stats.currentOdometer)
            assertDecimal("700", content.stats.cost.totalCost)
            assertEquals(1, content.rules.size)
            assertNotNull(content.stats.nextDue)
            assertEquals(3, content.sections.flatMap { it.items }.size)
        }

    @Test
    fun `a vehicle without history loads with empty sections, not an error`() =
        runTest {
            val viewModel = makeViewModel()

            viewModel.load()

            assertTrue(viewModel.content.sections.isEmpty())
            assertEquals(1_000.0, viewModel.content.stats.currentOdometer)
            assertNull(viewModel.content.stats.nextDue)
        }

    @Test
    fun `a repository failure surfaces as failed instead of being swallowed`() =
        runTest {
            val viewModel = makeViewModel(fuelFailure = RepositoryError.EntryNotFound(UUID.randomUUID()))

            viewModel.load()

            assertIs<State.Failed>(viewModel.state)
        }

    @Test
    fun `history is grouped by month, most recent first`() =
        runTest {
            val viewModel = makeViewModel(fuel = listOf(fuel(1, 1_000.0), fuel(40, 1_400.0)))

            viewModel.load()

            assertEquals(2, viewModel.content.sections.size)
            assertTrue(viewModel.content.sections[0].month > viewModel.content.sections[1].month)
        }

    @Test
    fun `rules are exposed once loaded and empty before`() =
        runTest {
            val rule = ServiceRule(vehicleId = vehicle.id, kind = ServiceKind.OIL_CHANGE, kilometerInterval = 10_000.0)
            val viewModel = makeViewModel(rules = listOf(rule))
            assertTrue(viewModel.rules.isEmpty())

            viewModel.load()

            assertEquals(listOf(rule), viewModel.rules)
        }

    @Test
    fun `timeline items expose the odometer of fuel and service entries`() {
        val fuelItem = VehicleTimeline.Item.Fuel(fuel(1, 1_000.0))
        val serviceItem = VehicleTimeline.Item.Service(ServiceEntry(vehicleId = vehicle.id, date = day(2), odometer = 1_100.0, cost = BigDecimal(50)))

        assertEquals(1_000.0, fuelItem.odometer)
        assertEquals(1_100.0, serviceItem.odometer)
    }

    @Test
    fun `odometer readings excluding an entry ignore that entry`() =
        runTest {
            val latest = fuel(40, 1_400.0)
            val viewModel = makeViewModel(fuel = listOf(fuel(1, 1_000.0), latest))
            viewModel.load()

            assertEquals(1_400.0, Odometer.current(viewModel.odometerReadings()))
            assertEquals(1_000.0, Odometer.current(viewModel.odometerReadings(VehicleTimeline.Item.Fuel(latest))))
        }

    @Test
    fun `deleting an entry reloads without it`() =
        runTest {
            val doomed = fuel(40, 1_400.0)
            val viewModel = makeViewModel(fuel = listOf(fuel(1, 1_000.0), doomed))
            viewModel.load()

            viewModel.delete(VehicleTimeline.Item.Fuel(doomed))

            assertEquals(
                1,
                viewModel.content.sections
                    .flatMap { it.items }
                    .size,
            )
            assertEquals(1_000.0, viewModel.content.stats.currentOdometer)
        }

    @Test
    fun `deleting a service entry that is gone surfaces the failure`() =
        runTest {
            val viewModel = makeViewModel()
            viewModel.load()

            viewModel.delete(VehicleTimeline.Item.Service(ServiceEntry(vehicleId = vehicle.id, date = day(2), odometer = 1_100.0, cost = BigDecimal(50))))

            assertIs<State.Failed>(viewModel.state)
        }

    @Test
    fun `the history filter hides the other record type and drops empty months`() =
        runTest {
            val viewModel =
                makeViewModel(
                    fuel = listOf(fuel(1, 1_000.0), fuel(40, 1_400.0)),
                    services =
                        listOf(
                            ServiceEntry(vehicleId = vehicle.id, kind = ServiceKind.OIL_CHANGE, date = day(2), odometer = 1_050.0, cost = BigDecimal(300)),
                        ),
                )
            assertTrue(viewModel.visibleSections.isEmpty())
            assertFalse(viewModel.hasHistory)
            assertNull(viewModel.lastPricePerLiter)

            viewModel.load()

            assertTrue(viewModel.hasHistory)
            assertDecimal("5", viewModel.lastPricePerLiter)
            assertEquals(3, viewModel.visibleSections.flatMap { it.items }.size)

            viewModel.filter = VehicleTimeline.Filter.SERVICE
            assertEquals(1, viewModel.visibleSections.size)
            assertEquals(1, viewModel.visibleSections.flatMap { it.items }.size)

            viewModel.filter = VehicleTimeline.Filter.FUEL
            assertEquals(2, viewModel.visibleSections.flatMap { it.items }.size)
        }

    @Test
    fun `spending covers the last six months`() =
        runTest {
            val viewModel = makeViewModel(fuel = listOf(fuel(100, 1_400.0)))

            viewModel.load()

            assertEquals(VehicleDetailViewModel.SPENDING_MONTHS, viewModel.content.spending.size)
            assertDecimal(
                "200",
                viewModel.content.spending
                    .map { it.total }
                    .sum(),
            )
        }
}
