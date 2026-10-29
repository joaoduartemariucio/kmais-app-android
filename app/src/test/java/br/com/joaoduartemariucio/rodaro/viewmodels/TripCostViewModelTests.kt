package br.com.joaoduartemariucio.rodaro.viewmodels

import br.com.joaoduartemariucio.rodaro.calculations.assertDecimal
import br.com.joaoduartemariucio.rodaro.calculations.day
import br.com.joaoduartemariucio.rodaro.calculations.decimal
import br.com.joaoduartemariucio.rodaro.calculations.full
import br.com.joaoduartemariucio.rodaro.calculations.testVehicleId
import br.com.joaoduartemariucio.rodaro.domain.common.errors.RepositoryError
import br.com.joaoduartemariucio.rodaro.domain.fueling.entities.FuelEntry
import br.com.joaoduartemariucio.rodaro.domain.preferences.entities.Preferences
import br.com.joaoduartemariucio.rodaro.domain.preferences.entities.UnitSystem
import br.com.joaoduartemariucio.rodaro.domain.trips.entities.SavedRoute
import br.com.joaoduartemariucio.rodaro.domain.trips.repositories.RouteRepository
import br.com.joaoduartemariucio.rodaro.domain.trips.services.RouteEstimateProvider
import br.com.joaoduartemariucio.rodaro.domain.vehicles.entities.Vehicle
import br.com.joaoduartemariucio.rodaro.fakes.FakeAnalytics
import br.com.joaoduartemariucio.rodaro.fakes.FakeRouteEstimateProvider
import br.com.joaoduartemariucio.rodaro.fakes.FixedDateProvider
import br.com.joaoduartemariucio.rodaro.fakes.InMemoryFuelEntryRepository
import br.com.joaoduartemariucio.rodaro.fakes.InMemoryRouteRepository
import br.com.joaoduartemariucio.rodaro.fakes.InMemoryVehicleRepository
import br.com.joaoduartemariucio.rodaro.presentation.common.DisplayPreferences
import br.com.joaoduartemariucio.rodaro.presentation.common.NumberInput
import br.com.joaoduartemariucio.rodaro.presentation.trips.TripCostViewModel
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TripCostViewModelTests {
    @After
    fun tearDown() {
        DisplayPreferences.update(Preferences())
    }

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
        routeRepository: RouteRepository = InMemoryRouteRepository(),
        routeEstimateProvider: RouteEstimateProvider = FakeRouteEstimateProvider(),
    ) = TripCostViewModel(
        vehicleRepository = InMemoryVehicleRepository(vehicles, failure),
        fuelEntryRepository = InMemoryFuelEntryRepository(fuel),
        routeRepository = routeRepository,
        routeEstimateProvider = routeEstimateProvider,
        dateProvider = FixedDateProvider(day(30)),
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

    @Test
    fun `imperial suggestions are converted back to the metric calculation`() =
        runTest {
            DisplayPreferences.update(Preferences(unitSystem = UnitSystem.IMPERIAL))
            val viewModel = makeViewModel()
            viewModel.load()

            assertEquals(12.5, viewModel.parsedConsumption!!, 0.01)
            assertEquals(6.1, viewModel.parsedPrice!!.toDouble(), 0.001)
        }

    @Test
    fun `miles, mpg and price per gallon are converted when the cost is computed`() =
        runTest {
            DisplayPreferences.update(Preferences(unitSystem = UnitSystem.IMPERIAL))
            val viewModel = makeViewModel(vehicles = emptyList(), fuel = emptyList())
            viewModel.load()
            viewModel.distance = NumberInput.text(100.0, NumberInput.Style.KILOMETERS)
            viewModel.consumption = NumberInput.text(10.0, NumberInput.Style.CONSUMPTION)
            viewModel.pricePerLiter = NumberInput.text(BigDecimal(4), NumberInput.Style.FUEL_PRICE)

            assertEquals(160.9344, viewModel.parsedDistance!!, 0.001)
            assertEquals(4.2517014, viewModel.parsedConsumption!!, 0.001)
            assertEquals(1.056688589, viewModel.parsedPrice!!.toDouble(), 0.000_001)
            assertNotNull(viewModel.cost)
        }

    @Test
    fun `looking up the real distance fills the trip and reports the toll flag`() =
        runTest {
            val analytics = FakeAnalytics()
            val provider = FakeRouteEstimateProvider()
            val viewModelWithAnalytics =
                TripCostViewModel(
                    vehicleRepository = InMemoryVehicleRepository(listOf(car)),
                    fuelEntryRepository = InMemoryFuelEntryRepository(carFuel),
                    routeRepository = InMemoryRouteRepository(),
                    routeEstimateProvider = provider,
                    dateProvider = FixedDateProvider(day(30)),
                    analytics = analytics,
                )
            viewModelWithAnalytics.load()
            viewModelWithAnalytics.origin = "São Paulo, SP"
            viewModelWithAnalytics.destination = "Rio de Janeiro, RJ"

            viewModelWithAnalytics.lookupRealDistance()

            assertEquals(listOf("São Paulo, SP" to "Rio de Janeiro, RJ"), provider.lookups)
            assertEquals(120.0, viewModelWithAnalytics.parsedDistance!!, 0.001)
            assertFalse(viewModelWithAnalytics.isRoundTrip)
            assertEquals(true, viewModelWithAnalytics.hasTolls)
            assertIs<TripCostViewModel.LookupState.Done>(viewModelWithAnalytics.lookupState)
            assertEquals("true", analytics.parameters("route_distance_looked_up")?.get("has_tolls"))
        }

    @Test
    fun `a failed lookup reports the error and keeps the distance`() =
        runTest {
            val provider = FakeRouteEstimateProvider(failure = java.io.IOException("offline"))
            val viewModel = makeViewModel(routeEstimateProvider = provider)
            viewModel.load()
            viewModel.origin = "São Paulo, SP"
            viewModel.destination = "Rio de Janeiro, RJ"

            viewModel.lookupRealDistance()

            assertIs<TripCostViewModel.LookupState.Failed>(viewModel.lookupState)
            assertEquals("", viewModel.distance)
            assertTrue(provider.lookups.isEmpty())
        }

    @Test
    fun `looking up without both places does nothing`() =
        runTest {
            val provider = FakeRouteEstimateProvider()
            val viewModel = makeViewModel(routeEstimateProvider = provider)
            viewModel.load()
            viewModel.origin = "São Paulo, SP"

            viewModel.lookupRealDistance()

            assertIs<TripCostViewModel.LookupState.Idle>(viewModel.lookupState)
            assertTrue(provider.lookups.isEmpty())
        }

    @Test
    fun `saving the calculation stores origin, destination and the snapshot`() =
        runTest {
            val analytics = FakeAnalytics()
            val repository = InMemoryRouteRepository()
            val viewModel =
                TripCostViewModel(
                    vehicleRepository = InMemoryVehicleRepository(listOf(car)),
                    fuelEntryRepository = InMemoryFuelEntryRepository(carFuel),
                    routeRepository = repository,
                    dateProvider = FixedDateProvider(day(30)),
                    analytics = analytics,
                )
            viewModel.load()
            viewModel.origin = "São Paulo, SP"
            viewModel.destination = "Rio de Janeiro, RJ"
            viewModel.distance = NumberInput.text(100.0, NumberInput.Style.KILOMETERS)
            viewModel.consumption = NumberInput.text(10.0, NumberInput.Style.CONSUMPTION)
            viewModel.pricePerLiter = NumberInput.text(BigDecimal(5), NumberInput.Style.FUEL_PRICE)
            viewModel.hasTolls = true

            assertTrue(viewModel.saveRoute())

            val saved = repository.routes().single()
            assertEquals("São Paulo, SP", saved.origin)
            assertEquals("Rio de Janeiro, RJ", saved.destination)
            assertEquals(100.0, saved.distanceKm, 0.001)
            assertEquals(testVehicleId, saved.vehicleId)
            assertEquals(day(30), saved.createdAt)
            assertTrue(saved.hasTolls)
            assertDecimal("50", saved.fuelCost)
            assertTrue(analytics.names().contains("route_saved"))
            assertEquals("true", analytics.parameters("route_saved")?.get("has_tolls"))
        }

    @Test
    fun `saving without a vehicle or without places is refused`() =
        runTest {
            val repository = InMemoryRouteRepository()
            val withoutVehicle =
                TripCostViewModel(
                    vehicleRepository = InMemoryVehicleRepository(),
                    fuelEntryRepository = InMemoryFuelEntryRepository(),
                    routeRepository = repository,
                    dateProvider = FixedDateProvider(day(30)),
                )
            withoutVehicle.load()
            withoutVehicle.distance = NumberInput.text(100.0, NumberInput.Style.KILOMETERS)
            withoutVehicle.consumption = NumberInput.text(10.0, NumberInput.Style.CONSUMPTION)
            withoutVehicle.pricePerLiter = NumberInput.text(BigDecimal(5), NumberInput.Style.FUEL_PRICE)
            assertFalse(withoutVehicle.saveRoute())

            val withoutPlaces = makeViewModel()
            withoutPlaces.load()
            withoutPlaces.distance = NumberInput.text(100.0, NumberInput.Style.KILOMETERS)
            withoutPlaces.consumption = NumberInput.text(10.0, NumberInput.Style.CONSUMPTION)
            withoutPlaces.pricePerLiter = NumberInput.text(BigDecimal(5), NumberInput.Style.FUEL_PRICE)
            assertFalse(withoutPlaces.saveRoute())

            assertTrue(repository.routes().isEmpty())
        }

    @Test
    fun `deleting a saved route removes it and reports the action`() =
        runTest {
            val analytics = FakeAnalytics()
            val route = savedRoute()
            val repository = InMemoryRouteRepository(listOf(route))
            val viewModel =
                TripCostViewModel(
                    vehicleRepository = InMemoryVehicleRepository(listOf(car)),
                    fuelEntryRepository = InMemoryFuelEntryRepository(carFuel),
                    routeRepository = repository,
                    dateProvider = FixedDateProvider(day(30)),
                    analytics = analytics,
                )
            viewModel.load()
            assertEquals(1, viewModel.savedRoutes.size)

            viewModel.deleteRoute(route.id)

            assertTrue(viewModel.savedRoutes.isEmpty())
            assertTrue(analytics.names().contains("route_deleted"))
        }

    @Test
    fun `restoring a saved route refills the form`() =
        runTest {
            val viewModel = makeViewModel()
            viewModel.load()

            viewModel.restoreRoute(savedRoute())

            assertEquals("São Paulo, SP", viewModel.origin)
            assertEquals("Rio de Janeiro, RJ", viewModel.destination)
            assertEquals(100.0, viewModel.parsedDistance!!, 0.001)
            assertTrue(viewModel.isRoundTrip)
            assertEquals(true, viewModel.hasTolls)
        }

    private fun savedRoute() =
        SavedRoute(
            id = UUID.randomUUID(),
            vehicleId = testVehicleId,
            origin = "São Paulo, SP",
            destination = "Rio de Janeiro, RJ",
            distanceKm = 100.0,
            isRoundTrip = true,
            hasTolls = true,
            liters = 20.0,
            fuelCost = BigDecimal("100"),
            costPerKilometer = BigDecimal("0.5"),
            createdAt = day(10),
        )
}
