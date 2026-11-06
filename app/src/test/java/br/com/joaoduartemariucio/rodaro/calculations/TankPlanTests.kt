package br.com.joaoduartemariucio.rodaro.calculations

import br.com.joaoduartemariucio.rodaro.domain.trips.calculations.TankPlan
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TankPlanTests {
    @Test
    fun `a trip shorter than one tank needs a single fill up and no stops`() {
        val plan = assertNotNull(TankPlan.of(liters = 20.0, kilometersPerLiter = 12.0, tankCapacity = 50.0))

        assertEquals(0.4, plan.tanks, 1e-9)
        assertEquals(1, plan.fillUps)
        assertEquals(0, plan.stopsOnTheWay)
        assertTrue(plan.fitsInOneTank)
    }

    @Test
    fun `a partial extra tank rounds up to another fill up`() {
        val plan = assertNotNull(TankPlan.of(liters = 80.0, kilometersPerLiter = 12.0, tankCapacity = 50.0))

        assertEquals(1.6, plan.tanks, 1e-9)
        assertEquals(2, plan.fillUps)
        assertEquals(1, plan.stopsOnTheWay)
        assertFalse(plan.fitsInOneTank)
    }

    @Test
    fun `exactly two tanks is two fill ups despite floating point noise`() {
        val plan = assertNotNull(TankPlan.of(liters = 100.00000001, kilometersPerLiter = 10.0, tankCapacity = 50.0))

        assertEquals(2, plan.fillUps)
    }

    @Test
    fun `range per tank is the capacity times the consumption`() {
        val plan = assertNotNull(TankPlan.of(liters = 10.0, kilometersPerLiter = 12.5, tankCapacity = 40.0))

        assertEquals(500.0, plan.rangePerTank, 1e-9)
    }

    @Test
    fun `no plan without a positive tank capacity, liters or consumption`() {
        assertNull(TankPlan.of(liters = 10.0, kilometersPerLiter = 12.0, tankCapacity = null))
        assertNull(TankPlan.of(liters = 10.0, kilometersPerLiter = 12.0, tankCapacity = 0.0))
        assertNull(TankPlan.of(liters = 0.0, kilometersPerLiter = 12.0, tankCapacity = 50.0))
        assertNull(TankPlan.of(liters = 10.0, kilometersPerLiter = 0.0, tankCapacity = 50.0))
    }
}
