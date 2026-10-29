package br.com.joaoduartemariucio.rodaro.calculations

import br.com.joaoduartemariucio.rodaro.domain.preferences.entities.CurrencyOption
import br.com.joaoduartemariucio.rodaro.domain.preferences.entities.Preferences
import br.com.joaoduartemariucio.rodaro.domain.preferences.entities.UnitSystem
import br.com.joaoduartemariucio.rodaro.fakes.TestLocalizer
import br.com.joaoduartemariucio.rodaro.presentation.common.DisplayPreferences
import br.com.joaoduartemariucio.rodaro.presentation.common.Formats
import br.com.joaoduartemariucio.rodaro.presentation.common.UnitConversion
import br.com.joaoduartemariucio.rodaro.presentation.common.formattedConsumption
import br.com.joaoduartemariucio.rodaro.presentation.common.formattedKilometers
import br.com.joaoduartemariucio.rodaro.presentation.common.formattedLiters
import org.junit.After
import org.junit.Test
import java.math.BigDecimal
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UnitConversionTests {
    private val localizer = TestLocalizer()

    @After
    fun tearDown() {
        DisplayPreferences.update(Preferences())
    }

    private fun imperial() {
        DisplayPreferences.update(Preferences(unitSystem = UnitSystem.IMPERIAL))
    }

    @Test
    fun `keeps every value untouched in the metric system`() {
        assertEquals(123.0, UnitConversion.distance(123.0))
        assertEquals(123.0, UnitConversion.distanceToKilometers(123.0))
        assertEquals(45.5, UnitConversion.volume(45.5))
        assertEquals(45.5, UnitConversion.volumeToLiters(45.5))
        assertEquals(12.4, UnitConversion.consumption(12.4))
        assertEquals(12.4, UnitConversion.consumptionToKilometersPerLiter(12.4))
        assertDecimal("7.89", UnitConversion.perDistance(BigDecimal("7.89")))
        assertDecimal("5.899", UnitConversion.perVolume(BigDecimal("5.899")))
        assertDecimal("5.899", UnitConversion.perVolumeToLiters(BigDecimal("5.899")))
    }

    @Test
    fun `converts distance back and forth between kilometers and miles`() {
        imperial()
        assertEquals(100.0, UnitConversion.distance(160.9344), 0.001)
        assertEquals(160.9344, UnitConversion.distanceToKilometers(100.0), 0.001)
    }

    @Test
    fun `converts volume back and forth between liters and gallons`() {
        imperial()
        assertEquals(0.2641720524, UnitConversion.volume(1.0), 0.0001)
        assertEquals(3.785411784, UnitConversion.volumeToLiters(1.0), 0.0001)
    }

    @Test
    fun `converts consumption back and forth between km per liter and mpg`() {
        imperial()
        assertEquals(23.5214584, UnitConversion.consumption(10.0), 0.001)
        assertEquals(4.2517014, UnitConversion.consumptionToKilometersPerLiter(10.0), 0.001)
        assertEquals(10.0, UnitConversion.consumptionToKilometersPerLiter(UnitConversion.consumption(10.0)), 0.000_001)
    }

    @Test
    fun `scales per-distance and per-volume money in the imperial system`() {
        imperial()
        assertDecimal("16.09344", UnitConversion.perDistance(BigDecimal("10")))
        assertDecimal("37.85411784", UnitConversion.perVolume(BigDecimal("10")))
        assertDecimal("10", UnitConversion.perVolumeToLiters(BigDecimal("37.85411784")))
    }

    @Test
    fun `shows distances in miles and volume in gallons when imperial`() {
        imperial()
        assertEquals("${Formats.number(100.0, 0)} mi", 160.9344.formattedKilometers())
        assertEquals("${Formats.number(10.0, 2)} gal", 37.85411784.formattedLiters())
    }

    @Test
    fun `keeps the metric suffixes when metric`() {
        assertEquals("${Formats.number(160.9344, 0)} km", 160.9344.formattedKilometers())
        assertEquals("${Formats.number(37.85, 2)} L", 37.85.formattedLiters())
    }

    @Test
    fun `renders consumption as mpg when imperial`() {
        imperial()
        val text = 6.21371192237334.formattedConsumption(localizer)
        assertTrue(text.endsWith("mpg"), text)
    }

    @Test
    fun `renders consumption as km per liter when metric`() {
        val text = 10.0.formattedConsumption(localizer)
        assertTrue(text.endsWith("km/L"), text)
    }

    @Test
    fun `formats money with the preferred currency regardless of the locale`() {
        Formats.currency(BigDecimal("12.34"), locale = Locale.US, override = CurrencyOption.EUR).let { text ->
            assertTrue(text.contains("12.34"), text)
            assertTrue(text.contains("€"), text)
        }
        Formats.currency(BigDecimal("12.34"), locale = Locale.US, override = null).let { text ->
            assertTrue(text.contains("$"), text)
        }
    }
}
