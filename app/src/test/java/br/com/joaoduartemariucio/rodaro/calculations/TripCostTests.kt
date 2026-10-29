package br.com.joaoduartemariucio.rodaro.calculations

import br.com.joaoduartemariucio.rodaro.domain.trips.calculations.TripCost
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TripCostTests {
    @Test
    fun `one way trip divides the distance by the consumption and multiplies by the price`() {
        val cost = assertNotNull(TripCost.of(120.0, isRoundTrip = false, kilometersPerLiter = 12.0, pricePerLiter = decimal("5.899")))

        assertEquals(120.0, cost.distance)
        assertEquals(10.0, cost.liters, 1e-9)
        assertDecimal("58.99", cost.fuelCost)
    }

    @Test
    fun `round trip doubles the distance`() {
        val cost = assertNotNull(TripCost.of(120.0, isRoundTrip = true, kilometersPerLiter = 12.0, pricePerLiter = decimal("6")))

        assertEquals(240.0, cost.distance)
        assertEquals(20.0, cost.liters, 1e-9)
        assertDecimal("120", cost.fuelCost)
    }

    @Test
    fun `cost per km is the fuel cost over the total distance`() {
        val cost = assertNotNull(TripCost.of(100.0, isRoundTrip = false, kilometersPerLiter = 10.0, pricePerLiter = decimal("6")))

        assertDecimal("0.6", cost.costPerKilometer)
    }

    @Test
    fun `no cost without a positive distance or consumption`() {
        assertNull(TripCost.of(0.0, isRoundTrip = false, kilometersPerLiter = 12.0, pricePerLiter = decimal("6")))
        assertNull(TripCost.of(100.0, isRoundTrip = false, kilometersPerLiter = 0.0, pricePerLiter = decimal("6")))
        assertNull(TripCost.of(100.0, isRoundTrip = false, kilometersPerLiter = 12.0, pricePerLiter = decimal("-1")))
    }
}
