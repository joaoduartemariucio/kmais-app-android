package br.com.joaoduartemariucio.rodaro.calculations

import br.com.joaoduartemariucio.rodaro.domain.fueling.calculations.Consumption
import br.com.joaoduartemariucio.rodaro.domain.fueling.entities.FuelEntry
import org.junit.Test
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

class ConsumptionTests {
    private data class Scenario(
        val name: String,
        val records: List<FuelEntry>,
        val intervals: List<Pair<Double, Double>>,
        val average: Double?,
    )

    private val scenarios =
        listOf(
            Scenario("empty history", emptyList(), emptyList(), null),
            Scenario("a single fill-up does not open a leg", listOf(full(0, 1_000.0, 40.0)), emptyList(), null),
            Scenario(
                "a sequence of partial fills only",
                listOf(partial(0, 1_000.0, 10.0), partial(5, 1_200.0, 12.0), partial(10, 1_400.0, 11.0)),
                emptyList(),
                null,
            ),
            Scenario("full tank to full tank", listOf(full(0, 1_000.0, 40.0), full(10, 1_400.0, 40.0)), listOf(400.0 to 40.0), 10.0),
            Scenario(
                "a partial fill before the first full tank is ignored",
                listOf(partial(0, 900.0, 10.0), full(5, 1_000.0, 40.0), full(15, 1_400.0, 40.0)),
                listOf(400.0 to 40.0),
                10.0,
            ),
            Scenario(
                "a partial fill in between adds liters to the leg",
                listOf(full(0, 1_000.0, 40.0), partial(5, 1_200.0, 20.0), full(10, 1_400.0, 20.0)),
                listOf(400.0 to 40.0),
                10.0,
            ),
            Scenario(
                "multiple partial fills within the same leg",
                listOf(
                    full(0, 1_000.0, 50.0),
                    partial(2, 1_100.0, 10.0),
                    partial(4, 1_200.0, 10.0),
                    partial(6, 1_300.0, 10.0),
                    full(8, 1_500.0, 20.0),
                ),
                listOf(500.0 to 50.0),
                10.0,
            ),
            Scenario(
                "two consecutive legs, weighted average",
                listOf(full(0, 1_000.0, 40.0), full(10, 1_400.0, 40.0), full(20, 1_800.0, 50.0)),
                listOf(400.0 to 40.0, 400.0 to 50.0),
                800.0 / 90.0,
            ),
            Scenario(
                "a backwards odometer discards only the affected leg",
                listOf(full(0, 1_000.0, 40.0), full(10, 900.0, 40.0), full(20, 1_400.0, 50.0)),
                listOf(500.0 to 50.0),
                10.0,
            ),
            Scenario(
                "an unchanged odometer between two full tanks is not a leg",
                listOf(full(0, 1_000.0, 40.0), full(10, 1_000.0, 30.0)),
                emptyList(),
                null,
            ),
            Scenario(
                "a record with non-positive liters is ignored",
                listOf(full(0, 1_000.0, 40.0), full(10, 1_400.0, 0.0)),
                emptyList(),
                null,
            ),
            Scenario(
                "an out-of-order array is sorted before calculating",
                listOf(full(10, 1_400.0, 40.0), full(0, 1_000.0, 40.0)),
                listOf(400.0 to 40.0),
                10.0,
            ),
        )

    @Test
    fun `intervals and average per scenario`() {
        for (scenario in scenarios) {
            val intervals = Consumption.intervals(scenario.records)

            assertEquals(scenario.intervals.size, intervals.size, scenario.name)
            intervals.zip(scenario.intervals).forEach { (actual, expected) ->
                assertEquals(expected.first, actual.distance, scenario.name)
                assertEquals(expected.second, actual.liters, scenario.name)
                assertEquals(expected.first / expected.second, actual.kilometersPerLiter, scenario.name)
            }

            val average = Consumption.average(scenario.records)
            when {
                average != null && scenario.average != null -> assertTrue(abs(average - scenario.average) < 1e-9, scenario.name)
                average == null && scenario.average == null -> Unit
                else -> fail("${scenario.name}: average $average != ${scenario.average}")
            }
        }
    }

    @Test
    fun `latest returns the most recent leg`() {
        val latest = Consumption.latest(listOf(full(0, 1_000.0, 40.0), full(10, 1_400.0, 40.0), full(20, 1_800.0, 50.0)))

        assertEquals(1_400.0, latest?.startOdometer)
        assertEquals(1_800.0, latest?.endOdometer)
        assertEquals(50.0, latest?.liters)
    }

    @Test
    fun `latest is null without a closed leg`() {
        assertNull(Consumption.latest(listOf(full(0, 1_000.0, 40.0))))
    }

    @Test
    fun `the leg carries the dates of the full tanks bounding it`() {
        val interval = Consumption.latest(listOf(full(3, 1_000.0, 40.0), full(13, 1_400.0, 40.0)))

        assertEquals(day(3), interval?.startDate)
        assertEquals(day(13), interval?.endDate)
    }
}
