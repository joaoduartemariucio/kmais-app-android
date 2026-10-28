package br.com.joaoduartemariucio.rodaro.calculations

import br.com.joaoduartemariucio.rodaro.domain.costs.calculations.Cost
import org.junit.Test
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class CostTests {
    @Test
    fun `no records - everything zeroed and no cost per kilometer`() {
        val summary = Cost.summary(emptyList(), emptyList())

        assertDecimal("0", summary.totalCost)
        assertEquals(0.0, summary.distance)
        assertNull(summary.costPerKilometer)
    }

    @Test
    fun `a single record has a known spend but no cost per kilometer`() {
        val summary = Cost.summary(listOf(full(0, 1_000.0, 40.0, price = BigDecimal(5))), emptyList())

        assertDecimal("200", summary.fuelCost)
        assertEquals(0.0, summary.distance)
        assertNull(summary.costPerKilometer)
    }

    @Test
    fun `records all at the same odometer define no distance`() {
        val summary = Cost.summary(listOf(full(0, 1_000.0, 40.0), full(10, 1_000.0, 40.0)), emptyList())

        assertEquals(0.0, summary.distance)
        assertNull(summary.costPerKilometer)
    }

    @Test
    fun `fuel and services add up in the cost per kilometer`() {
        val summary =
            Cost.summary(
                fuel = listOf(full(0, 1_000.0, 40.0, price = BigDecimal(5)), full(10, 1_400.0, 40.0, price = decimal("5.50"))),
                services = listOf(service(5, 1_200.0, BigDecimal(100))),
            )

        assertDecimal("420", summary.fuelCost)
        assertDecimal("100", summary.serviceCost)
        assertDecimal("520", summary.totalCost)
        assertEquals(400.0, summary.distance)
        assertDecimal("1.30", summary.costPerKilometer)
    }

    @Test
    fun `a service can be the far end of the odometer span`() {
        val summary =
            Cost.summary(
                fuel = listOf(full(0, 1_000.0, 40.0), full(10, 1_400.0, 40.0)),
                services = listOf(service(20, 1_600.0, BigDecimal(200))),
            )

        assertEquals(600.0, summary.distance)
    }

    @Test
    fun `summing cents does not accumulate floating-point error`() {
        val dime = decimal("0.10")
        val fuel = (0 until 10).map { full(it, 1_000.0 + it * 100, 1.0, price = dime) }

        val summary = Cost.summary(fuel, emptyList())
        val viaDouble = (0 until 10).fold(0.0) { total, _ -> total + 0.1 }

        assertDecimal("1", summary.fuelCost)
        assertNotEquals(1.0, viaDouble)
    }

    @Test
    fun `the period is whatever the lists carry - filtering by date changes the result`() {
        val everything = listOf(full(0, 1_000.0, 40.0), full(10, 1_400.0, 40.0), full(20, 1_800.0, 40.0))
        val recent = everything.filter { it.date >= day(10) }

        assertEquals(800.0, Cost.summary(everything, emptyList()).distance)
        assertEquals(400.0, Cost.summary(recent, emptyList()).distance)
    }
}
