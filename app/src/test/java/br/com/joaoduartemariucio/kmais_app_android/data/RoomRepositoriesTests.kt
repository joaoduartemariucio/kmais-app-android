package br.com.joaoduartemariucio.kmais_app_android.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import br.com.joaoduartemariucio.kmais_app_android.calculations.assertDecimal
import br.com.joaoduartemariucio.kmais_app_android.calculations.day
import br.com.joaoduartemariucio.kmais_app_android.data.models.KmaisDatabase
import br.com.joaoduartemariucio.kmais_app_android.data.repositories.RoomFuelEntryRepository
import br.com.joaoduartemariucio.kmais_app_android.data.repositories.RoomServiceRepository
import br.com.joaoduartemariucio.kmais_app_android.data.repositories.RoomVehicleRepository
import br.com.joaoduartemariucio.kmais_app_android.data.repositories.persisting
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.FuelEntry
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.ServiceEntry
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.ServiceKind
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.ServiceRule
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.Vehicle
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.VehicleKind
import br.com.joaoduartemariucio.kmais_app_android.domain.errors.RepositoryError
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class RoomRepositoriesTests {
    private val database = KmaisDatabase.create(ApplicationProvider.getApplicationContext<Context>(), inMemory = true)
    private val vehicles = RoomVehicleRepository(database.vehicles())
    private val fuel = RoomFuelEntryRepository(database.fuelEntries(), database.vehicles())
    private val services = RoomServiceRepository(database.services(), database.vehicles())

    private val gol =
        Vehicle(name = "Gol", kind = VehicleKind.CAR, plate = "ABC1D23", initialOdometer = 1_000.0, tankCapacity = 55.0, createdAt = day(0))

    @After
    fun close() = database.close()

    private fun fuelEntry(
        dayOffset: Int,
        odometer: Double,
        vehicleId: UUID = gol.id,
    ) = FuelEntry(
        vehicleId = vehicleId,
        date = day(dayOffset),
        odometer = odometer,
        liters = 40.5,
        pricePerLiter = BigDecimal("5.899"),
        isFullTank = true,
    )

    @Test
    fun `vehicles round-trip, sort newest first and hide archived ones`() =
        runTest {
            val cb = Vehicle(name = "CB 500", kind = VehicleKind.MOTORCYCLE, createdAt = day(10))
            vehicles.save(gol)
            vehicles.save(cb)

            assertEquals(listOf(cb, gol), vehicles.all(includingArchived = false))

            vehicles.setArchived(true, gol.id)
            assertEquals(listOf(cb), vehicles.all(includingArchived = false))
            assertEquals(listOf(cb.id, gol.id), vehicles.all(includingArchived = true).map { it.id })
            assertTrue(vehicles.vehicle(gol.id)?.isArchived == true)
            assertNull(vehicles.vehicle(UUID.randomUUID()))
        }

    @Test
    fun `saving an existing vehicle keeps its creation date`() =
        runTest {
            vehicles.save(gol)

            vehicles.save(gol.copy(name = "Gol G5", createdAt = day(99)))

            assertEquals(gol.copy(name = "Gol G5"), vehicles.vehicle(gol.id))
        }

    @Test
    fun `archiving a missing vehicle fails`() =
        runTest {
            assertIs<RepositoryError.VehicleNotFound>(assertFailsWith<RepositoryError> { vehicles.setArchived(true, UUID.randomUUID()) })
        }

    @Test
    fun `fuel entries round-trip in chronological order and keep exact prices`() =
        runTest {
            vehicles.save(gol)
            val later = fuelEntry(20, 1_800.0)
            val earlier = fuelEntry(10, 1_400.0)
            fuel.save(later)
            fuel.save(earlier)

            val saved = fuel.entries(gol.id)
            assertEquals(listOf(earlier.id, later.id), saved.map { it.id })
            assertDecimal("5.899", saved.first().pricePerLiter)
            assertEquals(40.5, saved.first().liters)

            fuel.save(earlier.copy(liters = 30.0))
            assertEquals(30.0, fuel.entries(gol.id).first().liters)

            fuel.delete(earlier.id)
            assertEquals(listOf(later.id), fuel.entries(gol.id).map { it.id })
        }

    @Test
    fun `a fuel entry needs an existing vehicle and deleting a missing one fails`() =
        runTest {
            assertIs<RepositoryError.VehicleNotFound>(assertFailsWith<RepositoryError> { fuel.save(fuelEntry(1, 1_000.0, UUID.randomUUID())) })
            assertIs<RepositoryError.EntryNotFound>(assertFailsWith<RepositoryError> { fuel.delete(UUID.randomUUID()) })
        }

    @Test
    fun `service entries round-trip and can be deleted`() =
        runTest {
            vehicles.save(gol)
            val oil =
                ServiceEntry(vehicleId = gol.id, kind = ServiceKind.OIL_CHANGE, date = day(5), odometer = 1_200.0, cost = BigDecimal("320.00"), notes = "5W30")
            services.save(oil)

            assertEquals(listOf(oil), services.entries(gol.id).map { it.copy(cost = it.cost.setScale(2)) })

            services.deleteEntry(oil.id)
            assertTrue(services.entries(gol.id).isEmpty())
            assertIs<RepositoryError.EntryNotFound>(assertFailsWith<RepositoryError> { services.deleteEntry(oil.id) })
            assertIs<RepositoryError.VehicleNotFound>(
                assertFailsWith<RepositoryError> { services.save(oil.copy(id = UUID.randomUUID(), vehicleId = UUID.randomUUID())) },
            )
        }

    @Test
    fun `saving rules replaces the vehicle's set`() =
        runTest {
            vehicles.save(gol)
            val oil = ServiceRule(vehicleId = gol.id, kind = ServiceKind.OIL_CHANGE, kilometerInterval = 10_000.0, monthInterval = 12)
            val tires = ServiceRule(vehicleId = gol.id, kind = ServiceKind.TIRES, kilometerInterval = 40_000.0)
            services.save(listOf(oil, tires), gol.id)

            assertEquals(setOf(oil, tires), services.rules(gol.id).toSet())

            val chain = ServiceRule(vehicleId = gol.id, kind = ServiceKind.CHAIN, kilometerInterval = 1_000.0)
            services.save(listOf(oil.copy(monthInterval = 6), chain), gol.id)
            assertEquals(setOf(oil.copy(monthInterval = 6), chain), services.rules(gol.id).toSet())

            services.save(emptyList(), gol.id)
            assertTrue(services.rules(gol.id).isEmpty())

            services.save(listOf(oil), gol.id)
            services.deleteRule(oil.id)
            assertTrue(services.rules(gol.id).isEmpty())
            assertIs<RepositoryError.EntryNotFound>(assertFailsWith<RepositoryError> { services.deleteRule(oil.id) })
            assertIs<RepositoryError.VehicleNotFound>(assertFailsWith<RepositoryError> { services.save(listOf(oil), UUID.randomUUID()) })
        }

    @Test
    fun `a storage failure is wrapped as a persistence error and domain errors pass through`() =
        runTest {
            val failure = assertFailsWith<RepositoryError> { persisting { throw IllegalStateException("disk full") } }
            assertIs<RepositoryError.PersistenceFailed>(failure)
            assertIs<IllegalStateException>(failure.cause)

            val missing = RepositoryError.EntryNotFound(UUID.randomUUID())
            assertEquals(missing, assertFailsWith<RepositoryError> { persisting { throw missing } })
        }
}
