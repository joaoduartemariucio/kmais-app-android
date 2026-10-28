package br.com.joaoduartemariucio.rodaro.calculations

import br.com.joaoduartemariucio.rodaro.domain.maintenance.calculations.Maintenance
import br.com.joaoduartemariucio.rodaro.domain.maintenance.calculations.MaintenanceForecast
import br.com.joaoduartemariucio.rodaro.domain.maintenance.calculations.MaintenanceForecast.Trigger
import br.com.joaoduartemariucio.rodaro.domain.maintenance.entities.ServiceEntry
import br.com.joaoduartemariucio.rodaro.domain.maintenance.entities.ServiceKind
import br.com.joaoduartemariucio.rodaro.domain.maintenance.entities.ServiceRule
import br.com.joaoduartemariucio.rodaro.domain.vehicles.entities.OdometerSample
import br.com.joaoduartemariucio.rodaro.domain.vehicles.entities.Vehicle
import org.junit.Test
import java.math.BigDecimal
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

class MaintenanceTests {
    private val history = listOf(sample(0, 10_000.0), sample(100, 15_000.0))
    private val lastServiceOdometer = 10_000.0
    private val kilometersPerMonth = 50 * 365.25 / 12

    @Test
    fun `average kilometers per month per scenario`() {
        val scenarios =
            listOf(
                Triple("empty history", emptyList(), null),
                Triple("a single sample", listOf(sample(0, 10_000.0)), null),
                Triple("two samples at the same instant", listOf(sample(0, 10_000.0), sample(0, 10_500.0)), null),
                Triple("a vehicle that has not moved", listOf(sample(0, 10_000.0), sample(100, 10_000.0)), null),
                Triple("50 km per day", history, kilometersPerMonth),
                Triple("out-of-order samples are sorted first", listOf(sample(100, 15_000.0), sample(0, 10_000.0)), kilometersPerMonth),
            )
        for ((name, samples, expected) in scenarios) {
            val average = Maintenance.averageKilometersPerMonth(samples)
            when {
                average != null && expected != null -> assertTrue(abs(average - expected) < 1e-9, name)
                average == null && expected == null -> Unit
                else -> fail("$name: average $average != $expected")
            }
        }
    }

    private fun forecast(
        kilometers: Double?,
        months: Int?,
        history: List<OdometerSample> = this.history,
    ): MaintenanceForecast? = Maintenance.forecast(rule(kilometers, months), day(0), lastServiceOdometer, history, utc)

    @Test
    fun `a rule with no interval forecasts nothing`() {
        assertNull(forecast(null, null))
        assertNull(forecast(0.0, 0))
    }

    @Test
    fun `a distance-only rule without usable history keeps its due km but has no date`() {
        val histories = listOf(emptyList(), listOf(sample(0, 10_000.0)), listOf(sample(0, 10_000.0), sample(100, 10_000.0)))
        for (history in histories) {
            val forecast = assertNotNull(forecast(10_000.0, null, history))
            assertNull(forecast.dueDate)
            assertEquals(20_000.0, forecast.dueOdometer)
            assertEquals(Trigger.DISTANCE, forecast.trigger)
        }
    }

    @Test
    fun `distance comes due before the calendar`() {
        val forecast = assertNotNull(forecast(10_000.0, 12))

        assertEquals(Trigger.DISTANCE, forecast.trigger)
        assertEquals(day(200), forecast.dueDate)
        assertEquals(20_000.0, forecast.dueOdometer)
        assertTrue(abs(assertNotNull(forecast.averageKilometersPerMonth) - kilometersPerMonth) < 1e-9)
    }

    @Test
    fun `months come due before the distance`() {
        val forecast = assertNotNull(forecast(30_000.0, 6))

        assertEquals(Trigger.CALENDAR, forecast.trigger)
        assertEquals(day(181), forecast.dueDate)
        assertEquals(40_000.0, forecast.dueOdometer)
    }

    @Test
    fun `a time-only rule has no due odometer`() {
        val forecast = assertNotNull(forecast(null, 6))

        assertEquals(Trigger.CALENDAR, forecast.trigger)
        assertEquals(day(181), forecast.dueDate)
        assertNull(forecast.dueOdometer)
    }

    @Test
    fun `a distance-only rule projects the date from the usage pace`() {
        val forecast = assertNotNull(forecast(10_000.0, null))

        assertEquals(Trigger.DISTANCE, forecast.trigger)
        assertEquals(day(200), forecast.dueDate)
    }

    @Test
    fun `a tie between distance and calendar resolves to distance`() {
        val forecast = assertNotNull(forecast(18_250.0, 12))

        assertEquals(day(365), forecast.dueDate)
        assertEquals(Trigger.DISTANCE, forecast.trigger)
    }

    @Test
    fun `an already overdue service returns a date in the past`() {
        val forecast = assertNotNull(forecast(1_000.0, 12))

        assertEquals(Trigger.DISTANCE, forecast.trigger)
        assertEquals(day(20), forecast.dueDate)
        assertTrue(assertNotNull(forecast.dueDate) < day(100))
    }

    @Test
    fun `a vehicle that has not moved falls back to the time rule`() {
        val forecast = assertNotNull(forecast(10_000.0, 6, listOf(sample(0, 10_000.0), sample(100, 10_000.0))))

        assertEquals(Trigger.CALENDAR, forecast.trigger)
        assertEquals(day(181), forecast.dueDate)
        assertEquals(20_000.0, forecast.dueOdometer)
        assertNull(forecast.averageKilometersPerMonth)
    }
}

class NextServiceDueTests {
    private val vehicle = Vehicle(name = "Gol", initialOdometer = 10_000.0, createdAt = day(0))
    private val history = listOf(sample(0, 10_000.0), sample(100, 15_000.0))

    private fun rule(
        kind: ServiceKind,
        kilometers: Double?,
        months: Int?,
    ) = ServiceRule(vehicleId = vehicle.id, kind = kind, kilometerInterval = kilometers, monthInterval = months)

    private fun service(kind: ServiceKind) = ServiceEntry(vehicleId = vehicle.id, kind = kind, date = day(50), odometer = 12_500.0, cost = BigDecimal(300))

    @Test
    fun `without rules there is nothing due`() {
        assertNull(Maintenance.nextDue(emptyList(), vehicle, emptyList(), history, utc))
    }

    @Test
    fun `with no service of that kind the baseline is the vehicle registration`() {
        val due = assertNotNull(Maintenance.due(rule(ServiceKind.OIL_CHANGE, 10_000.0, null), vehicle, emptyList(), history, utc))

        assertEquals(20_000.0, due.forecast.dueOdometer)
        assertEquals(day(200), due.forecast.dueDate)
    }

    @Test
    fun `the last service of the kind wins over the registration`() {
        val due =
            assertNotNull(
                Maintenance.due(rule(ServiceKind.OIL_CHANGE, 10_000.0, null), vehicle, listOf(service(ServiceKind.OIL_CHANGE)), history, utc),
            )

        assertEquals(22_500.0, due.forecast.dueOdometer)
    }

    @Test
    fun `a service of another kind does not move the baseline`() {
        val due =
            assertNotNull(Maintenance.due(rule(ServiceKind.OIL_CHANGE, 10_000.0, null), vehicle, listOf(service(ServiceKind.TIRES)), history, utc))

        assertEquals(20_000.0, due.forecast.dueOdometer)
    }

    @Test
    fun `the earliest due date wins among several rules`() {
        val due =
            assertNotNull(
                Maintenance.nextDue(
                    listOf(
                        rule(ServiceKind.OIL_CHANGE, 10_000.0, null),
                        rule(ServiceKind.BRAKES, null, 6),
                        rule(ServiceKind.TIRES, 40_000.0, null),
                    ),
                    vehicle,
                    emptyList(),
                    history,
                    utc,
                ),
            )

        assertEquals(ServiceKind.BRAKES, due.rule.kind)
        assertEquals(day(181), due.forecast.dueDate)
    }

    @Test
    fun `a rule with no possible forecast is skipped instead of blocking the others`() {
        val due =
            assertNotNull(
                Maintenance.nextDue(
                    listOf(rule(ServiceKind.TIRES, null, null), rule(ServiceKind.OIL_CHANGE, 10_000.0, null)),
                    vehicle,
                    emptyList(),
                    history,
                    utc,
                ),
            )

        assertEquals(ServiceKind.OIL_CHANGE, due.rule.kind)
    }

    @Test
    fun `an overdue rule beats a future one`() {
        val due =
            assertNotNull(
                Maintenance.nextDue(
                    listOf(rule(ServiceKind.OIL_CHANGE, 10_000.0, null), rule(ServiceKind.CHAIN, 1_000.0, null)),
                    vehicle,
                    emptyList(),
                    history,
                    utc,
                ),
            )

        assertEquals(ServiceKind.CHAIN, due.rule.kind)
        assertTrue(assertNotNull(due.forecast.dueDate) < day(100))
    }

    @Test
    fun `a tie on the due date resolves deterministically by kind`() {
        val due =
            assertNotNull(
                Maintenance.nextDue(
                    listOf(rule(ServiceKind.TIRES, 10_000.0, null), rule(ServiceKind.BRAKES, 10_000.0, null)),
                    vehicle,
                    emptyList(),
                    history,
                    utc,
                ),
            )

        assertEquals(ServiceKind.BRAKES, due.rule.kind)
    }
}
