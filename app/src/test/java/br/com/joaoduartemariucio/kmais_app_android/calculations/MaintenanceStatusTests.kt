package br.com.joaoduartemariucio.kmais_app_android.calculations

import br.com.joaoduartemariucio.kmais_app_android.domain.calculations.Maintenance
import br.com.joaoduartemariucio.kmais_app_android.domain.calculations.MaintenanceForecast
import br.com.joaoduartemariucio.kmais_app_android.domain.calculations.MaintenanceForecast.Trigger
import br.com.joaoduartemariucio.kmais_app_android.domain.calculations.MaintenanceStatus
import br.com.joaoduartemariucio.kmais_app_android.domain.calculations.MaintenanceStatus.Level
import br.com.joaoduartemariucio.kmais_app_android.domain.calculations.ServiceDue
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.OdometerSample
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.ServiceEntry
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.ServiceKind
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.Vehicle
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class MaintenanceStatusTests {
    private fun due(
        kilometers: Double?,
        months: Int?,
        dueDate: Instant,
        trigger: Trigger = Trigger.DISTANCE,
        since: OdometerSample = sample(0, 10_000.0),
    ) = ServiceDue(
        rule = rule(kilometers, months),
        forecast = MaintenanceForecast(dueDate, trigger, kilometers?.let { since.odometer + it }, null),
        since = since,
    )

    private fun status(
        due: ServiceDue,
        odometer: Double,
        now: Int,
    ): MaintenanceStatus = Maintenance.status(due, odometer, day(now), utc)

    @Test
    fun `halfway through both intervals is on track`() {
        val result = status(due(10_000.0, 12, day(200)), 15_000.0, 100)

        assertEquals(Level.ON_TRACK, result.level)
        assertEquals(0.5, result.progress)
        assertEquals(5_000.0, result.remainingKilometers)
        assertEquals(100, result.remainingDays)
    }

    @Test
    fun `a short interval uses the 300 km floor`() {
        val chain = due(1_000.0, null, day(300))

        assertEquals(Level.ON_TRACK, status(chain, 10_650.0, 10).level)
        assertEquals(Level.DUE_SOON, status(chain, 10_750.0, 10).level)
    }

    @Test
    fun `a long interval is capped at 1,000 km`() {
        val plugs = due(40_000.0, null, day(3_000))

        assertEquals(Level.ON_TRACK, status(plugs, 48_500.0, 10).level)
        assertEquals(Level.DUE_SOON, status(plugs, 49_100.0, 10).level)
    }

    @Test
    fun `the last days before the due date are due soon`() {
        val oil = due(null, 12, day(365), Trigger.CALENDAR)

        assertEquals(Level.ON_TRACK, status(oil, 10_000.0, 330).level)
        assertEquals(Level.DUE_SOON, status(oil, 10_000.0, 340).level)
    }

    @Test
    fun `reaching the due odometer is overdue`() {
        val result = status(due(10_000.0, 12, day(300)), 21_000.0, 100)

        assertEquals(Level.OVERDUE, result.level)
        assertEquals(1.0, result.progress)
        assertEquals(-1_000.0, result.remainingKilometers)
    }

    @Test
    fun `passing the calendar due date is overdue`() {
        val result = status(due(null, 1, day(31), Trigger.CALENDAR), 10_000.0, 40)

        assertEquals(Level.OVERDUE, result.level)
        assertEquals(-9, result.remainingDays)
    }

    @Test
    fun `a pace-based date in the past is only due soon while the odometer is short`() {
        val result = status(due(10_000.0, null, day(50)), 12_000.0, 60)

        assertEquals(Level.DUE_SOON, result.level)
        assertEquals(-10, result.remainingDays)
    }

    @Test
    fun `due starts counting from the last service of that kind`() {
        val vehicle = Vehicle(name = "Gol", initialOdometer = 1_000.0, createdAt = day(0))
        val oil = ServiceEntry(vehicleId = vehicle.id, kind = ServiceKind.OIL_CHANGE, date = day(20), odometer = 2_000.0, cost = BigDecimal(100))
        val history = listOf(sample(0, 1_000.0), sample(40, 3_000.0))

        assertEquals(sample(20, 2_000.0), Maintenance.due(rule(5_000.0, null), vehicle, listOf(oil), history, utc)?.since)
        assertEquals(sample(0, 1_000.0), Maintenance.due(rule(5_000.0, null), vehicle, emptyList(), history, utc)?.since)
    }

    @Test
    fun `the status says which limit made it overdue and keeps the calendar date`() {
        val both = due(10_000.0, 1, day(31), Trigger.CALENDAR)

        val byCalendar = status(both, 12_000.0, 40)
        assertEquals(Trigger.CALENDAR, byCalendar.overdueBy)
        assertEquals(day(31), byCalendar.calendarDueDate)

        assertEquals(Trigger.CALENDAR, status(both, 20_000.0, 40).overdueBy)
        assertEquals(Trigger.DISTANCE, status(both, 20_000.0, 20).overdueBy)
        assertNull(status(both, 12_000.0, 10).overdueBy)
    }

    @Test
    fun `without a forecast date the status comes from km alone`() {
        val chain =
            ServiceDue(
                rule = rule(1_000.0, null),
                forecast = MaintenanceForecast(null, Trigger.DISTANCE, 11_000.0, null),
                since = sample(0, 10_000.0),
            )

        val early = status(chain, 10_200.0, 5)
        assertEquals(Level.ON_TRACK, early.level)
        assertNull(early.remainingDays)
        assertEquals(0.2, early.progress, 1e-12)
        assertFalse(early.isEstimatePassed)

        assertEquals(Level.DUE_SOON, status(chain, 10_800.0, 5).level)
        assertEquals(Trigger.DISTANCE, status(chain, 11_000.0, 5).overdueBy)
    }
}
