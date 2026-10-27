package br.com.joaoduartemariucio.kmais_app_android.presentation.common

import br.com.joaoduartemariucio.kmais_app_android.R
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Currency
import java.util.Locale

object Formats {
    fun number(
        value: Number,
        fractionDigits: Int,
        locale: Locale = Locale.getDefault(),
    ): String =
        NumberFormat
            .getNumberInstance(locale)
            .apply {
                minimumFractionDigits = fractionDigits
                maximumFractionDigits = fractionDigits
            }.format(value)

    fun currency(
        value: BigDecimal,
        fractionDigits: Int? = null,
        locale: Locale = Locale.getDefault(),
    ): String =
        NumberFormat
            .getCurrencyInstance(locale)
            .apply {
                currency = currencyFor(locale)
                if (fractionDigits != null) {
                    minimumFractionDigits = fractionDigits
                    maximumFractionDigits = fractionDigits
                }
            }.format(value)

    fun currencyFor(locale: Locale): Currency = runCatching { Currency.getInstance(locale) }.getOrNull() ?: Currency.getInstance("BRL")

    fun date(
        instant: Instant,
        skeleton: String,
        zone: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.getDefault(),
    ): String {
        val pattern =
            runCatching {
                android.text.format.DateFormat
                    .getBestDateTimePattern(locale, skeleton)
            }.getOrNull() ?: skeleton
        return DateTimeFormatter.ofPattern(pattern, locale).format(instant.atZone(zone))
    }
}

fun Double.formattedKilometers(fractionDigits: Int = 0): String = "${Formats.number(this, fractionDigits)} km"

fun Double.formattedLiters(fractionDigits: Int = 2): String = "${Formats.number(this, fractionDigits)} L"

fun Double.formattedConsumption(localizer: Localizer): String = localizer.string(R.string.format_consumption, Formats.number(this, 1))

fun Double.formattedKilometersPerMonth(localizer: Localizer): String = localizer.string(R.string.format_per_month, formattedKilometers())

fun Double.formattedRemainingKilometers(localizer: Localizer): String =
    if (this > 0) {
        localizer.string(R.string.format_km_left, formattedKilometers())
    } else {
        localizer.string(R.string.format_km_past_due, (-this).formattedKilometers())
    }

fun Int.formattedRelativeDays(localizer: Localizer): String =
    when {
        this == 0 -> localizer.string(R.string.relative_today)
        this == 1 -> localizer.string(R.string.relative_tomorrow)
        this == -1 -> localizer.string(R.string.relative_yesterday)
        this > 0 -> localizer.plural(R.plurals.relative_in_days, this, this)
        else -> localizer.plural(R.plurals.relative_days_ago, -this, -this)
    }

val BigDecimal.formattedCurrency: String get() = Formats.currency(this)

fun BigDecimal.formattedPerKilometer(localizer: Localizer): String = localizer.string(R.string.format_per_km, formattedCurrency)

fun BigDecimal.formattedPerLiter(localizer: Localizer): String = localizer.string(R.string.format_per_liter, formattedCurrency)

fun Instant.formattedDay(zone: ZoneId = ZoneId.systemDefault()): String = Formats.date(this, "dMMM", zone)

fun Instant.formattedFullDate(zone: ZoneId = ZoneId.systemDefault()): String =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(Locale.getDefault()).format(atZone(zone))

fun Instant.formattedMonthAndYear(zone: ZoneId = ZoneId.systemDefault()): String = Formats.date(this, "MMMMyyyy", zone)

fun Instant.formattedShortMonth(zone: ZoneId = ZoneId.systemDefault()): String = Formats.date(this, "MMM", zone)
