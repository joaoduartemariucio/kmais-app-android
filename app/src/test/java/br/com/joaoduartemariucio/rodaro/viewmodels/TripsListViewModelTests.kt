package br.com.joaoduartemariucio.rodaro.viewmodels

import br.com.joaoduartemariucio.rodaro.calculations.assertDecimal
import br.com.joaoduartemariucio.rodaro.calculations.day
import br.com.joaoduartemariucio.rodaro.calculations.decimal
import br.com.joaoduartemariucio.rodaro.calculations.testVehicleId
import br.com.joaoduartemariucio.rodaro.domain.common.errors.RepositoryError
import br.com.joaoduartemariucio.rodaro.domain.trips.entities.SavedRoute
import br.com.joaoduartemariucio.rodaro.domain.trips.repositories.RouteRepository
import br.com.joaoduartemariucio.rodaro.domain.vehicles.entities.Vehicle
import br.com.joaoduartemariucio.rodaro.fakes.FakeAnalytics
import br.com.joaoduartemariucio.rodaro.fakes.InMemoryRouteRepository
import br.com.joaoduartemariucio.rodaro.fakes.InMemoryVehicleRepository
import br.com.joaoduartemariucio.rodaro.presentation.trips.TripsListViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TripsListViewModelTests {
    @Test
    fun `load shows saved routes`() =
        runTest {
            val later = savedRoute(day(2))
            val earlier = savedRoute(day(1))
            val viewModel = TripsListViewModel(routeRepository = InMemoryRouteRepository(listOf(earlier, later)), vehicleRepository = InMemoryVehicleRepository())

            viewModel.load()

            assertIs<TripsListViewModel.State.Loaded>(viewModel.state)
            assertEquals(listOf(later, earlier), viewModel.routes)
        }

    @Test
    fun `load failure is exposed`() =
        runTest {
            val failingRepository =
                object : RouteRepository {
                    override suspend fun routes(vehicleId: UUID?): List<SavedRoute> = throw RepositoryError.PersistenceFailed(IllegalStateException("disk"))

                    override suspend fun save(route: SavedRoute) = Unit

                    override suspend fun delete(id: UUID) = Unit
                }
            val viewModel = TripsListViewModel(routeRepository = failingRepository, vehicleRepository = InMemoryVehicleRepository())

            viewModel.load()

            assertIs<TripsListViewModel.State.Failed>(viewModel.state)
        }

    @Test
    fun `delete removes the route and reports analytics`() =
        runTest {
            val analytics = FakeAnalytics()
            val first = savedRoute(day(1))
            val second = savedRoute(day(2))
            val repository = InMemoryRouteRepository(listOf(first, second))
            val viewModel = TripsListViewModel(routeRepository = repository, vehicleRepository = InMemoryVehicleRepository(), analytics = analytics)
            viewModel.load()

            viewModel.deleteRoute(second)

            assertEquals(listOf(first), viewModel.routes)
            assertNull(viewModel.actionError)
            assertTrue(analytics.names().contains("route_deleted"))
        }

    @Test
    fun `routes can be filtered by vehicle with totals for the visible ones`() =
        runTest {
            val car = Vehicle(id = testVehicleId, name = "Car", createdAt = day(0))
            val bike = Vehicle(name = "Bike", createdAt = day(1))
            val carRoute = savedRoute(day(1))
            val bikeRoute = savedRoute(day(2)).copy(vehicleId = bike.id, isRoundTrip = true, fuelCost = decimal("30"))
            val viewModel =
                TripsListViewModel(
                    routeRepository = InMemoryRouteRepository(listOf(carRoute, bikeRoute)),
                    vehicleRepository = InMemoryVehicleRepository(listOf(car, bike)),
                )
            viewModel.load()

            assertEquals(setOf(car, bike), viewModel.filterVehicles.toSet())
            assertEquals(car, viewModel.vehicle(carRoute))
            assertDecimal("80", viewModel.visibleTotalCost)
            assertEquals(300.0, viewModel.visibleTotalDistance, 1e-9)

            viewModel.filter(bike.id)

            assertEquals(listOf(bikeRoute), viewModel.visibleRoutes)
            assertDecimal("30", viewModel.visibleTotalCost)
            assertEquals(200.0, viewModel.visibleTotalDistance, 1e-9)
        }

    @Test
    fun `deleting the last route of the filtered vehicle clears the filter`() =
        runTest {
            val bike = Vehicle(name = "Bike", createdAt = day(1))
            val carRoute = savedRoute(day(1))
            val bikeRoute = savedRoute(day(2)).copy(vehicleId = bike.id)
            val viewModel =
                TripsListViewModel(
                    routeRepository = InMemoryRouteRepository(listOf(carRoute, bikeRoute)),
                    vehicleRepository = InMemoryVehicleRepository(listOf(bike)),
                )
            viewModel.load()
            viewModel.filter(bike.id)

            viewModel.deleteRoute(bikeRoute)

            assertNull(viewModel.vehicleFilter)
            assertEquals(listOf(carRoute), viewModel.visibleRoutes)
        }

    private fun savedRoute(createdAt: java.time.Instant) =
        SavedRoute(
            id = UUID.randomUUID(),
            vehicleId = testVehicleId,
            origin = "São Paulo, SP",
            destination = "Rio de Janeiro, RJ",
            distanceKm = 100.0,
            isRoundTrip = false,
            hasTolls = true,
            tollCost = BigDecimal.ZERO,
            kilometersPerLiter = 10.0,
            pricePerLiter = BigDecimal("5"),
            liters = 10.0,
            fuelCost = BigDecimal("50"),
            costPerKilometer = BigDecimal("0.5"),
            createdAt = createdAt,
        )
}
