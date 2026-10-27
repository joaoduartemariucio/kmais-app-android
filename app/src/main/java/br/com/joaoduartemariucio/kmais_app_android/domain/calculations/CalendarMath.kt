package br.com.joaoduartemariucio.kmais_app_android.domain.calculations

import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

fun Instant.startOfDay(zone: ZoneId): Instant = atZone(zone).toLocalDate().atStartOfDay(zone).toInstant()

fun Instant.plusDays(
    days: Long,
    zone: ZoneId,
): Instant = atZone(zone).plusDays(days).toInstant()

fun Instant.plusMonths(
    months: Long,
    zone: ZoneId,
): Instant = atZone(zone).plusMonths(months).toInstant()

fun Instant.atHour(
    hour: Int,
    zone: ZoneId,
): Instant =
    atZone(zone)
        .toLocalDate()
        .atTime(hour, 0)
        .atZone(zone)
        .toInstant()

fun Instant.monthStart(zone: ZoneId): Instant =
    atZone(zone)
        .toLocalDate()
        .withDayOfMonth(1)
        .atStartOfDay(zone)
        .toInstant()

fun daysBetween(
    start: Instant,
    end: Instant,
    zone: ZoneId,
): Int = ChronoUnit.DAYS.between(start.atZone(zone).toLocalDate(), end.atZone(zone).toLocalDate()).toInt()
