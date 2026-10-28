package br.com.joaoduartemariucio.rodaro.presentation.common

import java.math.BigDecimal
import java.util.Locale

object NumberInput {
    sealed interface Style {
        val fractionDigits: Int

        data object Integer : Style {
            override val fractionDigits = 0
        }

        data class Decimal(
            override val fractionDigits: Int,
        ) : Style

        data class Currency(
            override val fractionDigits: Int,
        ) : Style

        companion object {
            val KILOMETERS: Style = Integer
            val MONTHS: Style = Integer
            val LITERS: Style = Decimal(2)
            val FUEL_PRICE: Style = Currency(3)
            val CONSUMPTION: Style = Decimal(1)
            val MONEY: Style = Currency(2)
        }
    }

    private const val MAX_DIGITS = 12

    fun masked(
        text: String,
        previous: String,
        style: Style,
    ): String {
        var digits = digits(text)
        if (decimal(text, style)?.let { text(it, style) } == text) {
            return text
        }
        if (text.length < previous.length && digits == digits(previous)) {
            digits = digits.dropLast(1)
        }
        val value = value(digits.take(MAX_DIGITS), style) ?: return ""
        return text(value, style)
    }

    fun decimal(
        text: String,
        style: Style,
    ): BigDecimal? = value(digits(text), style)

    fun double(
        text: String,
        style: Style,
    ): Double? = decimal(text, style)?.toDouble()

    fun text(
        value: Double?,
        style: Style,
    ): String = text(value?.let { if (it.isFinite()) BigDecimal(it.toString()) else BigDecimal.ZERO }, style)

    fun text(
        value: BigDecimal?,
        style: Style,
        locale: Locale = Locale.getDefault(),
    ): String {
        value ?: return ""
        return when (style) {
            is Style.Integer, is Style.Decimal -> Formats.number(value, style.fractionDigits, locale)
            is Style.Currency -> Formats.currency(value, style.fractionDigits, locale)
        }
    }

    private fun digits(text: String): String = text.filter { it in '0'..'9' }

    private fun value(
        digits: String,
        style: Style,
    ): BigDecimal? {
        if (digits.isEmpty()) return null
        return BigDecimal(digits).movePointLeft(style.fractionDigits)
    }
}
