package br.com.joaoduartemariucio.rodaro.viewmodels

import br.com.joaoduartemariucio.rodaro.calculations.day
import br.com.joaoduartemariucio.rodaro.calculations.testVehicleId
import br.com.joaoduartemariucio.rodaro.domain.common.errors.RepositoryError
import br.com.joaoduartemariucio.rodaro.domain.trips.entities.SavedRoute
import br.com.joaoduartemariucio.rodaro.domain.trips.repositories.RouteRepository
import br.com.joaoduartemariucio.rodaro.domain.vehicles.entities.Vehicle
import br.com.joaoduartemariucio.rodaro.fakes.FakeAnalytics
import br.com.joaoduartemariucio.rodaro.fakes.InMemoryRouteRepository
import br.com.joaoduartemariucio.rodaro.fakes.InMemoryVehicleRepository
import br.com.joaoduartemariucio.rodaro.presentation.trips.RouteDetailViewModel
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

class RouteDetailViewModelTests {
    private val car = Vehicle(id = testVehicleId, name = "Car", tankCapacity = 50.0, createdAt = day(0))

    @Test
    fun `load shows the route with its vehicle and tank plan`() =
        runTest {
            val route = savedRoute()
            val viewModel = makeViewModel(InMemoryRouteRepository(listOf(route)))

            viewModel.load(route.id)

            assertEquals(route, assertIs<RouteDetailViewModel.State.Loaded>(viewModel.state).route)
            assertEquals(car, viewModel.vehicle)
            val plan = assertNotNull(viewModel.tankPlan)
            assertEquals(1.6, plan.tanks, 1e-9)
            assertEquals(2, plan.fillUps)
        }

    @Test
    fun `a vehicle without tank capacity has no tank plan`() =
        runTest {
            val route = savedRoute()
            val viewModel =
                RouteDetailViewModel(
                    routeRepository = InMemoryRouteRepository(listOf(route)),
                    vehicleRepository = InMemoryVehicleRepository(listOf(car.copy(tankCapacity = null))),
                )

            viewModel.load(route.id)

            assertNull(viewModel.tankPlan)
        }

    @Test
    fun `an unknown route is missing`() =
        runTest {
            val viewModel = makeViewModel(InMemoryRouteRepository())

            viewModel.load(UUID.randomUUID())

            assertIs<RouteDetailViewModel.State.Missing>(viewModel.state)
            assertNull(viewModel.route)
            assertNull(viewModel.tankPlan)
            assertFalse(viewModel.delete())
        }

    @Test
    fun `a storage failure is shown`() =
        runTest {
            val viewModel = makeViewModel(FailingRouteRepository())

            viewModel.load(UUID.randomUUID())

            assertIs<RouteDetailViewModel.State.Failed>(viewModel.state)
        }

    @Test
    fun `delete removes the route and reports analytics`() =
        runTest {
            val route = savedRoute()
            val repository = InMemoryRouteRepository(listOf(route))
            val analytics = FakeAnalytics()
            val viewModel = RouteDetailViewModel(repository, InMemoryVehicleRepository(listOf(car)), analytics)
            viewModel.load(route.id)

            assertTrue(viewModel.delete())

            assertTrue(repository.routes().isEmpty())
            assertTrue(analytics.names().contains("route_deleted"))
        }

    @Test
    fun `a failed delete is exposed`() =
        runTest {
            val route = savedRoute()
            val repository = FailingRouteRepository(listOf(route))
            val viewModel = makeViewModel(repository)
            viewModel.load(route.id)
            repository.failing = true

            assertFalse(viewModel.delete())

            assertNotNull(viewModel.actionError)
        }

    private fun makeViewModel(repository: RouteRepository) = RouteDetailViewModel(routeRepository = repository, vehicleRepository = InMemoryVehicleRepository(listOf(car)))

    private class FailingRouteRepository(
        private val stored: List<SavedRoute> = emptyList(),
        var failing: Boolean = stored.isEmpty(),
    ) : RouteRepository {
        override suspend fun routes(vehicleId: UUID?): List<SavedRoute> = if (failing) throw failure() else stored

        override suspend fun save(route: SavedRoute) = throw failure()

        override suspend fun delete(id: UUID) = throw failure()

        private fun failure() = RepositoryError.PersistenceFailed(IllegalStateException("disk"))
    }

    private fun savedRoute() =
        SavedRoute(
            id = UUID.randomUUID(),
            vehicleId = testVehicleId,
            origin = "São Paulo, SP",
            destination = "Curitiba, PR",
            distanceKm = 400.0,
            isRoundTrip = true,
            hasTolls = true,
            tollCost = BigDecimal.ZERO,
            kilometersPerLiter = 10.0,
            pricePerLiter = BigDecimal("6"),
            liters = 80.0,
            fuelCost = BigDecimal("480"),
            costPerKilometer = BigDecimal("0.6"),
            createdAt = day(5),
        )
}
