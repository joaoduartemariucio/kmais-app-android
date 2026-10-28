package br.com.joaoduartemariucio.rodaro.calculations

import br.com.joaoduartemariucio.rodaro.domain.common.calculations.atHour
import br.com.joaoduartemariucio.rodaro.domain.fueling.entities.FuelEntry
import br.com.joaoduartemariucio.rodaro.domain.maintenance.calculations.MaintenanceForecast
import br.com.joaoduartemariucio.rodaro.domain.maintenance.calculations.MaintenanceForecast.Trigger
import br.com.joaoduartemariucio.rodaro.domain.maintenance.calculations.MaintenanceStatus
import br.com.joaoduartemariucio.rodaro.domain.maintenance.calculations.ServiceDue
import br.com.joaoduartemariucio.rodaro.domain.maintenance.entities.ServiceEntry
import br.com.joaoduartemariucio.rodaro.domain.maintenance.entities.ServiceKind
import br.com.joaoduartemariucio.rodaro.domain.maintenance.entities.ServiceRule
import br.com.joaoduartemariucio.rodaro.domain.reminders.calculations.ReminderPlan
import br.com.joaoduartemariucio.rodaro.domain.vehicles.entities.Vehicle
import br.com.joaoduartemariucio.rodaro.domain.vehicles.entities.VehicleRecords
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ReminderPlanTests {
    private val vehicle = Vehicle(id = testVehicleId, name = "Gol", initialOdometer = 10_000.0, createdAt = day(0))

    private fun records(
        vehicle: Vehicle = this.vehicle,
        fuel: List<FuelEntry> = emptyList(),
        services: List<ServiceEntry> = emptyList(),
        rules: List<ServiceRule>,
    ) = VehicleRecords(vehicle, fuel, services, rules)

    private fun rule(
        kind: ServiceKind,
        kilometers: Double? = null,
        months: Int? = null,
    ) = ServiceRule(vehicleId = testVehicleId, kind = kind, kilometerInterval = kilometers, monthInterval = months)

    @Test
    fun `fires at 9 on the due day when it is still ahead`() {
        assertEquals(hour(9, 10), ReminderPlan.fireDate(day(10), hour(15, 2), utc))
    }

    @Test
    fun `an overdue reminder fires at the next 9`() {
        assertEquals(hour(9, 5), ReminderPlan.fireDate(day(1), hour(7, 5), utc))
        assertEquals(hour(9, 6), ReminderPlan.fireDate(day(1), hour(10, 5), utc))
    }

    @Test
    fun `a calendar rule is due months after the last service of its kind`() {
        val rule = rule(ServiceKind.OIL_CHANGE, months = 6)

        val planned = ReminderPlan.reminders(listOf(records(rules = listOf(rule))), day(1), utc)

        val reminder = planned.first()
        assertEquals(1, planned.size)
        assertEquals(rule.id, reminder.id)
        assertEquals(Trigger.CALENDAR, reminder.due.forecast.trigger)
        assertEquals(assertNotNull(reminder.due.forecast.dueDate).atHour(9, utc), reminder.fireDate)
        assertFalse(reminder.isOverdue)
    }

    @Test
    fun `a reminder past its due date is flagged as overdue`() {
        val planned = ReminderPlan.reminders(listOf(records(rules = listOf(rule(ServiceKind.INSPECTION, months = 1)))), day(90), utc)

        assertTrue(planned.first().isOverdue)
    }

    @Test
    fun `an overdue reminder repeats daily for a week, then weekly`() {
        assertEquals(hour(9, 4), ReminderPlan.overdueFireDate(day(0), hour(10, 3), utc))
        assertEquals(hour(9, 7), ReminderPlan.overdueFireDate(day(0), hour(10, 6), utc))
        assertEquals(hour(9, 14), ReminderPlan.overdueFireDate(day(0), hour(10, 7), utc))
        assertEquals(hour(9, 14), ReminderPlan.overdueFireDate(day(0), hour(8, 14), utc))
        assertEquals(hour(9, 21), ReminderPlan.overdueFireDate(day(0), hour(10, 14), utc))
    }

    @Test
    fun `a passed pace estimate is not repeated until the service is really overdue`() {
        val fuel = listOf(full(10, 10_500.0, 30.0), full(20, 11_000.0, 30.0))

        assertTrue(
            ReminderPlan.reminders(listOf(records(fuel = fuel, rules = listOf(rule(ServiceKind.OIL_CHANGE, 5_000.0)))), day(200), utc).isEmpty(),
        )
    }

    @Test
    fun `a km-only rule without a pace is notified once the odometer reaches it`() {
        val vehicle = Vehicle(id = testVehicleId, name = "CB", initialOdometer = 10_000.0, createdAt = day(5))

        val planned =
            ReminderPlan.reminders(
                listOf(records(vehicle = vehicle, fuel = listOf(full(5, 11_200.0, 10.0)), rules = listOf(rule(ServiceKind.CHAIN, 1_000.0)))),
                hour(10, 5),
                utc,
            )

        val reminder = planned.first()
        assertTrue(reminder.isOverdue)
        assertEquals(Trigger.DISTANCE, reminder.status.overdueBy)
        assertEquals(11_200.0, reminder.currentOdometer)
        assertEquals(hour(9, 6), reminder.fireDate)
    }

    @Test
    fun `overdue by km counts from the first reading at or past the due km`() {
        val fuel = listOf(full(10, 10_600.0, 10.0), full(20, 11_100.0, 10.0), full(30, 11_500.0, 10.0))

        val reminder =
            ReminderPlan.reminders(listOf(records(fuel = fuel, rules = listOf(rule(ServiceKind.CHAIN, 1_000.0)))), hour(10, 40), utc).first()

        assertEquals(
            day(20),
            ReminderPlan.overdueSince(
                reminder.due,
                reminder.status,
                listOf(sample(0, 10_000.0), sample(10, 10_600.0), sample(20, 11_100.0), sample(30, 11_500.0)),
                day(40),
                utc,
            ),
        )
        assertEquals(hour(9, 41), reminder.fireDate)
    }

    @Test
    fun `a passed pace estimate still reminds on the calendar due day`() {
        val fuel = listOf(full(10, 10_500.0, 30.0), full(20, 11_000.0, 30.0))

        val reminder =
            ReminderPlan.reminders(listOf(records(fuel = fuel, rules = listOf(rule(ServiceKind.OIL_CHANGE, 5_000.0, 12)))), day(200), utc).first()

        assertFalse(reminder.isOverdue)
        assertEquals(hour(9, 365), reminder.fireDate)
    }

    @Test
    fun `overdue both ways counts from the earlier date`() {
        val fuel = listOf(full(10, 11_200.0, 10.0), full(50, 11_400.0, 10.0))

        val reminder =
            ReminderPlan.reminders(listOf(records(fuel = fuel, rules = listOf(rule(ServiceKind.OIL_CHANGE, 1_000.0, 1)))), hour(10, 60), utc).first()

        assertEquals(Trigger.CALENDAR, reminder.status.overdueBy)
        assertEquals(
            day(10),
            ReminderPlan.overdueSince(
                reminder.due,
                reminder.status,
                listOf(sample(0, 10_000.0), sample(10, 11_200.0), sample(50, 11_400.0)),
                hour(10, 60),
                utc,
            ),
        )
    }

    @Test
    fun `without a reading past the due km, overdue counts from the latest reading`() {
        val due =
            ServiceDue(
                rule = rule(ServiceKind.CHAIN, 1_000.0),
                forecast = MaintenanceForecast(null, Trigger.DISTANCE, 11_000.0, null),
                since = sample(30, 10_000.0),
            )
        val status = MaintenanceStatus(MaintenanceStatus.Level.OVERDUE, 1.0, -200.0, null, Trigger.DISTANCE)

        assertEquals(day(20), ReminderPlan.overdueSince(due, status, listOf(sample(0, 10_000.0), sample(20, 11_200.0)), day(40), utc))
        assertEquals(day(40), ReminderPlan.overdueSince(due, status, emptyList(), day(40), utc))
    }

    @Test
    fun `a distance rule without a km pace yet has nothing to schedule`() {
        assertTrue(ReminderPlan.reminders(listOf(records(rules = listOf(rule(ServiceKind.TIRES, 20_000.0)))), day(1), utc).isEmpty())
    }

    @Test
    fun `archived vehicles get no reminders`() {
        val archived = vehicle.copy(isArchived = true)

        assertTrue(
            ReminderPlan.reminders(listOf(records(vehicle = archived, rules = listOf(rule(ServiceKind.OIL_CHANGE, months = 6)))), day(1), utc).isEmpty(),
        )
    }

    @Test
    fun `reminders come out ordered by fire date`() {
        val later = rule(ServiceKind.OIL_CHANGE, months = 12)
        val sooner = rule(ServiceKind.INSPECTION, months = 3)

        val planned = ReminderPlan.reminders(listOf(records(rules = listOf(later, sooner))), day(1), utc)

        assertEquals(listOf(sooner.id, later.id), planned.map { it.id })
    }
}
