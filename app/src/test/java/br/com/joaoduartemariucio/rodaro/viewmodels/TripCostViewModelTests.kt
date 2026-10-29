package br.com.joaoduartemariucio.rodaro.viewmodels

import br.com.joaoduartemariucio.rodaro.calculations.assertDecimal
import br.com.joaoduartemariucio.rodaro.calculations.day
import br.com.joaoduartemariucio.rodaro.calculations.decimal
import br.com.joaoduartemariucio.rodaro.calculations.full
import br.com.joaoduartemariucio.rodaro.calculations.testVehicleId
import br.com.joaoduartemariucio.rodaro.domain.common.errors.RepositoryError
import br.com.joaoduartemariucio.rodaro.domain.fueling.entities.FuelEntry
import br.com.joaoduartemariucio.rodaro.domain.vehicles.entities.Vehicle
import br.com.joaoduartemariucio.rodaro.fakes.InMemoryFuelEntryRepository
import br.com.joaoduartemariucio.rodaro.fakes.InMemoryVehicleRepository
import br.com.joaoduartemariucio.rodaro.presentation.common.NumberInput
import br.com.joaoduartemariucio.rodaro.presentation.trips.TripCostViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TripCostViewModelTests {
    private val car = Vehicle(id = testVehicleId, name = "Car", createdAt = day(0))
    private val bike = Vehicle(name = "Bike", createdAt = day(1))

    private val carFuel =
        listOf(
            full(0, 1_000.0, 40.0, decimal("5.5")),
            full(10, 1_500.0, 40.0, decimal("6.1")),
        )

    private fun makeViewModel(
        vehicles: List<Vehicle> = listOf(car),
        fuel: List<FuelEntry> = carFuel,
        failure: Exception? = null,
    ) = TripCostViewModel(
        vehicleRepository = InMemoryVehicleRepository(vehicles, failure),
        fuelEntryRepository = InMemoryFuelEntryRepository(fuel),
    )

    @Test
    fun `load picks a vehicle and fills its average consumption and last price`() =
        runTest {
            val viewModel = makeViewModel()

            viewModel.load()

            assertIs<TripCostViewModel.State.Loaded>(viewModel.state)
            assertEquals(car, viewModel.selectedVehicle)
            assertEquals(12.5, viewModel.parsedConsumption)
            assertDecimal("6.1", viewModel.parsedPrice)
            assertTrue(viewModel.isUsingAverageConsumption)
            assertTrue(viewModel.isUsingLastPrice)
        }

    @Test
    fun `cost uses the distance, round trip and the filled values`() =
        runTest {
            val viewModel = makeViewModel()
            viewModel.load()

            viewModel.distance = NumberInput.text(250.0, NumberInput.Style.KILOMETERS)
            viewModel.isRoundTrip = true

            val cost = assertNotNull(viewModel.cost)
            assertEquals(500.0, cost.distance)
            assertEquals(40.0, cost.liters, 1e-9)
            assertDecimal("244", cost.fuelCost)
        }

    @Test
    fun `no cost until a distance is entered`() =
        runTest {
            val viewModel = makeViewModel()
            viewModel.load()

            assertNull(viewModel.cost)
        }

    @Test
    fun `a vehicle without history leaves consumption for the user`() =
        runTest {
            val viewModel = makeViewModel(vehicles = listOf(bike), fuel = emptyList())

            viewModel.load()

            assertEquals(bike, viewModel.selectedVehicle)
            assertEquals("", viewModel.consumption)
            assertEquals("", viewModel.pricePerLiter)
            assertFalse(viewModel.isUsingAverageConsumption)
        }

    @Test
    fun `switching vehicles keeps values the user typed`() =
        runTest {
            val viewModel = makeViewModel(vehicles = listOf(car, bike))
            viewModel.load()
            viewModel.select(car)
            val typed = NumberInput.text(9.0, NumberInput.Style.CONSUMPTION)
            viewModel.consumption = typed

            viewModel.select(bike)

            assertEquals(typed, viewModel.consumption)
            assertEquals("", viewModel.pricePerLiter)
        }

    @Test
    fun `switching vehicles replaces the suggested values`() =
        runTest {
            val viewModel = makeViewModel(vehicles = listOf(car, bike))
            viewModel.load()
            viewModel.select(bike)

            viewModel.select(car)

            assertEquals(12.5, viewModel.parsedConsumption)
            assertDecimal("6.1", viewModel.parsedPrice)
        }

    @Test
    fun `without vehicles the calculator still works by hand`() =
        runTest {
            val viewModel = makeViewModel(vehicles = emptyList(), fuel = emptyList())

            viewModel.load()

            assertIs<TripCostViewModel.State.Loaded>(viewModel.state)
            assertNull(viewModel.selectedVehicle)
        }

    @Test
    fun `a storage failure is shown`() =
        runTest {
            val viewModel = makeViewModel(failure = RepositoryError.PersistenceFailed(IllegalStateException()))

            viewModel.load()

            assertIs<TripCostViewModel.State.Failed>(viewModel.state)
        }
}
