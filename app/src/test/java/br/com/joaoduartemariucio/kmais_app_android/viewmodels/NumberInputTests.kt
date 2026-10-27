package br.com.joaoduartemariucio.kmais_app_android.viewmodels

import br.com.joaoduartemariucio.kmais_app_android.presentation.common.NumberInput
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.NumberInput.Style
import org.junit.Test
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NumberInputTests {
    private fun assertSame(
        expected: BigDecimal,
        actual: BigDecimal?,
    ) = assertEquals(0, expected.compareTo(actual), "expected $expected, got $actual")

    @Test
    fun `typed digits shift into the fraction digits of each style`() {
        val cases =
            listOf(
                Triple(Style.KILOMETERS, "42350", BigDecimal(42_350)),
                Triple(Style.LITERS, "4050", BigDecimal("40.5")),
                Triple(Style.FUEL_PRICE, "5899", BigDecimal("5.899")),
                Triple(Style.MONEY, "15000", BigDecimal(150)),
            )
        for ((style, typed, expected) in cases) {
            val masked = NumberInput.masked(typed, "", style)

            assertEquals(NumberInput.text(expected, style), masked)
            assertSame(expected, NumberInput.decimal(masked, style))
        }
    }

    @Test
    fun `an empty field stays empty and distinct from zero`() {
        assertTrue(NumberInput.masked("", "1", Style.LITERS).isEmpty())
        assertNull(NumberInput.decimal("", Style.LITERS))
        assertSame(BigDecimal.ZERO, NumberInput.decimal(NumberInput.masked("0", "", Style.LITERS), Style.LITERS))
    }

    @Test
    fun `formatted text is left untouched`() {
        val formatted = NumberInput.text(BigDecimal("5.899"), Style.FUEL_PRICE)

        assertEquals(formatted, NumberInput.masked(formatted, "", Style.FUEL_PRICE))
    }

    @Test
    fun `deleting a non-digit character removes the last digit instead`() {
        val formatted = NumberInput.text(BigDecimal("40.5"), Style.LITERS)

        val masked = NumberInput.masked("$formatted ", "$formatted L", Style.LITERS)

        assertSame(BigDecimal("4.05"), NumberInput.decimal(masked, Style.LITERS))
    }

    @Test
    fun `edit mode round-trips a stored value`() {
        assertEquals(42_000.0, NumberInput.double(NumberInput.text(42_000.0, Style.KILOMETERS), Style.KILOMETERS))
    }

    @Test
    fun `input is capped at twelve digits`() {
        val masked = NumberInput.masked("12345678901234", "", Style.KILOMETERS)

        assertSame(BigDecimal(123_456_789_012), NumberInput.decimal(masked, Style.KILOMETERS))
    }
}
