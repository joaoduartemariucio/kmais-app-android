package br.com.joaoduartemariucio.rodaro.viewmodels

import br.com.joaoduartemariucio.rodaro.calculations.day
import br.com.joaoduartemariucio.rodaro.calculations.sample
import br.com.joaoduartemariucio.rodaro.calculations.testVehicleId
import br.com.joaoduartemariucio.rodaro.domain.common.errors.RepositoryError
import br.com.joaoduartemariucio.rodaro.domain.maintenance.calculations.MaintenanceForecast
import br.com.joaoduartemariucio.rodaro.domain.maintenance.calculations.MaintenanceForecast.Trigger
import br.com.joaoduartemariucio.rodaro.domain.maintenance.calculations.MaintenanceStatus
import br.com.joaoduartemariucio.rodaro.domain.maintenance.calculations.ServiceDue
import br.com.joaoduartemariucio.rodaro.domain.maintenance.entities.ServiceKind
import br.com.joaoduartemariucio.rodaro.domain.maintenance.entities.ServiceRule
import br.com.joaoduartemariucio.rodaro.domain.preferences.entities.Preferences
import br.com.joaoduartemariucio.rodaro.domain.reminders.calculations.PlannedReminder
import br.com.joaoduartemariucio.rodaro.domain.reminders.services.Reminder
import br.com.joaoduartemariucio.rodaro.domain.vehicles.entities.Vehicle
import br.com.joaoduartemariucio.rodaro.fakes.FakeReminderScheduler
import br.com.joaoduartemariucio.rodaro.fakes.FixedDateProvider
import br.com.joaoduartemariucio.rodaro.fakes.InMemoryFuelEntryRepository
import br.com.joaoduartemariucio.rodaro.fakes.InMemoryPreferencesRepository
import br.com.joaoduartemariucio.rodaro.fakes.InMemoryServiceRepository
import br.com.joaoduartemariucio.rodaro.fakes.InMemoryVehicleRepository
import br.com.joaoduartemariucio.rodaro.fakes.TestLocalizer
import br.com.joaoduartemariucio.rodaro.presentation.common.formattedFullDate
import br.com.joaoduartemariucio.rodaro.presentation.common.formattedKilometers
import br.com.joaoduartemariucio.rodaro.presentation.reminders.ServiceReminderSync
import br.com.joaoduartemariucio.rodaro.presentation.vehicles.detail.VehicleDetailViewModel
import br.com.joaoduartemariucio.rodaro.presentation.vehicles.list.VehicleListViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ServiceReminderSyncTests {
    private val vehicle = Vehicle(id = testVehicleId, name = "Gol", initialOdometer = 10_000.0, createdAt = day(0))
    private val oilRule = ServiceRule(vehicleId = testVehicleId, kind = ServiceKind.OIL_CHANGE, monthInterval = 6)

    private fun makeSync(
        vehicles: List<Vehicle> = listOf(vehicle),
        rules: List<ServiceRule> = listOf(oilRule),
        scheduler: FakeReminderScheduler = FakeReminderScheduler(),
        vehicleFailure: Exception? = null,
        preferences: InMemoryPreferencesRepository = InMemoryPreferencesRepository(),
    ) = ServiceReminderSync(
        vehicleRepository = InMemoryVehicleRepository(vehicles, vehicleFailure),
        fuelEntryRepository = InMemoryFuelEntryRepository(),
        serviceRepository = InMemoryServiceRepository(rules = rules),
        scheduler = scheduler,
        dateProvider = FixedDateProvider(day(1)),
        localizer = TestLocalizer(),
        preferences = preferences,
    )

    private fun stale(id: UUID = UUID.randomUUID()) = Reminder(id, "old", "old", day(2))

    @Test
    fun `cancels everything and stops when reminders are disabled`() =
        runTest {
            val scheduler = FakeReminderScheduler(pending = listOf(stale()))
            val preferences = InMemoryPreferencesRepository(stored = Preferences(remindersEnabled = false))

            makeSync(scheduler = scheduler, preferences = preferences).sync()

            assertTrue(scheduler.scheduled.isEmpty())
            assertEquals(0, scheduler.authorizationRequests)
        }

    @Test
    fun `schedules one reminder per rule with a known due date`() =
        runTest {
            val scheduler = FakeReminderScheduler()

            makeSync(scheduler = scheduler).sync()

            assertEquals(setOf(oilRule.id), scheduler.scheduled.keys)
            assertEquals("Gol: Oil change", scheduler.scheduled[oilRule.id]?.title)
            assertFalse(scheduler.scheduled[oilRule.id]?.body.isNullOrEmpty())
            assertEquals(1, scheduler.authorizationRequests)
        }

    @Test
    fun `cancels reminders whose rule no longer applies`() =
        runTest {
            val leftover = stale()
            val scheduler = FakeReminderScheduler(listOf(leftover))

            makeSync(scheduler = scheduler).sync()

            assertFalse(leftover.id in scheduler.scheduled)
            assertTrue(oilRule.id in scheduler.scheduled)
        }

    @Test
    fun `without anything to schedule, it cancels and never asks for permission`() =
        runTest {
            val scheduler = FakeReminderScheduler(listOf(stale()))

            makeSync(rules = emptyList(), scheduler = scheduler).sync()

            assertTrue(scheduler.scheduled.isEmpty())
            assertEquals(0, scheduler.authorizationRequests)
        }

    @Test
    fun `when notifications are denied, nothing is scheduled`() =
        runTest {
            val scheduler = FakeReminderScheduler(isAuthorized = false)

            makeSync(scheduler = scheduler).sync()

            assertTrue(scheduler.scheduled.isEmpty())
        }

    @Test
    fun `a repository failure leaves pending reminders untouched`() =
        runTest {
            val pending = stale(oilRule.id)
            val scheduler = FakeReminderScheduler(listOf(pending))

            makeSync(scheduler = scheduler, vehicleFailure = RepositoryError.VehicleNotFound(testVehicleId)).sync()

            assertEquals(mapOf(pending.id to pending), scheduler.scheduled.toMap())
        }

    private fun planned(
        dueDate: Instant?,
        trigger: Trigger,
        dueOdometer: Double?,
        overdueBy: Trigger? = null,
    ) = PlannedReminder(
        vehicle = vehicle,
        due = ServiceDue(oilRule, MaintenanceForecast(dueDate, trigger, dueOdometer, null), sample(0, 10_000.0)),
        status =
            MaintenanceStatus(
                level = if (overdueBy == null) MaintenanceStatus.Level.ON_TRACK else MaintenanceStatus.Level.OVERDUE,
                progress = 0.5,
                remainingKilometers = null,
                remainingDays = null,
                overdueBy = overdueBy,
                calendarDueDate = day(8),
            ),
        currentOdometer = 21_234.0,
        fireDate = day(10),
    )

    @Test
    fun `the body says whether the service is overdue by km or date, or due by distance or date`() {
        val sync = makeSync()
        val byDate = sync.reminder(planned(day(10), Trigger.CALENDAR, null))
        val byDistance = sync.reminder(planned(day(10), Trigger.DISTANCE, 20_000.0))
        val overdueByDate = sync.reminder(planned(day(10), Trigger.CALENDAR, null, Trigger.CALENDAR))
        val overdueByDistance = sync.reminder(planned(null, Trigger.DISTANCE, 20_000.0, Trigger.DISTANCE))
        val dateless = sync.reminder(planned(null, Trigger.DISTANCE, 20_000.0))

        assertEquals(oilRule.id, byDate.id)
        assertEquals(day(10), byDate.fireDate)
        assertTrue(byDate.title.contains(vehicle.name))
        assertEquals(5, listOf(byDate, byDistance, overdueByDate, overdueByDistance, dateless).map { it.body }.toSet().size)
        assertTrue(byDistance.body.contains(20_000.0.formattedKilometers()))
        assertTrue(overdueByDistance.body.contains(21_234.0.formattedKilometers()))
        assertTrue(overdueByDate.body.contains(day(8).formattedFullDate(FixedDateProvider(day(1)).zone)))
    }

    @Test
    fun `loading the list and the detail syncs reminders`() =
        runTest {
            val scheduler = FakeReminderScheduler()
            val sync = makeSync(scheduler = scheduler)
            val list =
                VehicleListViewModel(
                    scope = VehicleListViewModel.Scope.ACTIVE,
                    vehicleRepository = InMemoryVehicleRepository(listOf(vehicle)),
                    fuelEntryRepository = InMemoryFuelEntryRepository(),
                    serviceRepository = InMemoryServiceRepository(),
                    dateProvider = FixedDateProvider(day(30)),
                    reminders = sync,
                )

            list.load()
            assertEquals(1, scheduler.scheduled.size)

            scheduler.cancel(oilRule.id)
            val detail =
                VehicleDetailViewModel(
                    vehicle = vehicle,
                    fuelEntryRepository = InMemoryFuelEntryRepository(),
                    serviceRepository = InMemoryServiceRepository(),
                    dateProvider = FixedDateProvider(day(1)),
                    reminders = sync,
                )

            detail.load()
            assertEquals(1, scheduler.scheduled.size)
        }
}
