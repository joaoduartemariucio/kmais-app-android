package br.com.joaoduartemariucio.kmais_app_android.domain.calculations

import br.com.joaoduartemariucio.kmais_app_android.domain.entities.OdometerSample
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.ServiceEntry
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.ServiceRule
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.Vehicle
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.sortedChronologically
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.floor

object Maintenance {
    const val AVERAGE_DAYS_PER_MONTH = 365.25 / 12
    const val DUE_SOON_FRACTION = 0.15
    val dueSoonKilometers = 300.0..1_000.0
    val dueSoonDays = 15.0..30.0

    fun averageKilometersPerDay(samples: List<OdometerSample>): Double? {
        val sorted = samples.sortedChronologically()
        val first = sorted.firstOrNull() ?: return null
        val last = sorted.last()

        val days = Duration.between(first.date, last.date).toMillis() / 86_400_000.0
        val kilometers = last.odometer - first.odometer
        if (days <= 0 || kilometers <= 0) return null

        return kilometers / days
    }

    fun averageKilometersPerMonth(samples: List<OdometerSample>): Double? =
        averageKilometersPerDay(samples)?.let { it * AVERAGE_DAYS_PER_MONTH }

    fun forecast(
        rule: ServiceRule,
        lastServiceDate: Instant,
        lastServiceOdometer: Double,
        history: List<OdometerSample>,
        zone: ZoneId = ZoneId.systemDefault(),
    ): MaintenanceForecast? {
        val kilometersPerDay = averageKilometersPerDay(history)
        val kilometersPerMonth = kilometersPerDay?.let { it * AVERAGE_DAYS_PER_MONTH }

        var dueOdometer: Double? = null
        var dateByDistance: Instant? = null

        val interval = rule.kilometerInterval
        if (interval != null && interval > 0) {
            val due = lastServiceOdometer + interval
            dueOdometer = due

            val current = history.sortedChronologically().lastOrNull()
            if (kilometersPerDay != null && current != null) {
                val days = (due - current.odometer) / kilometersPerDay
                if (days.isFinite() && abs(days) < 36_525) {
                    dateByDistance = current.date.plusDays(roundedAwayFromZero(days), zone)
                }
            }
        }

        val months = rule.monthInterval
        val dateByCalendar = if (months != null && months > 0) lastServiceDate.plusMonths(months.toLong(), zone) else null

        val dueDate: Instant?
        val trigger: MaintenanceForecast.Trigger
        when {
            dateByDistance != null && dateByCalendar != null -> {
                dueDate = minOf(dateByDistance, dateByCalendar)
                trigger =
                    if (dateByDistance <= dateByCalendar) MaintenanceForecast.Trigger.DISTANCE else MaintenanceForecast.Trigger.CALENDAR
            }

            dateByDistance != null -> {
                dueDate = dateByDistance
                trigger = MaintenanceForecast.Trigger.DISTANCE
            }

            dateByCalendar != null -> {
                dueDate = dateByCalendar
                trigger = MaintenanceForecast.Trigger.CALENDAR
            }

            else -> {
                if (dueOdometer == null) return null
                dueDate = null
                trigger = MaintenanceForecast.Trigger.DISTANCE
            }
        }

        return MaintenanceForecast(
            dueDate = dueDate,
            trigger = trigger,
            dueOdometer = dueOdometer,
            averageKilometersPerMonth = kilometersPerMonth,
        )
    }

    fun due(
        rule: ServiceRule,
        vehicle: Vehicle,
        services: List<ServiceEntry>,
        history: List<OdometerSample>,
        zone: ZoneId = ZoneId.systemDefault(),
    ): ServiceDue? {
        val last = services.filter { it.kind == rule.kind }.maxByOrNull { it.date }

        val since =
            OdometerSample(
                date = last?.date ?: vehicle.createdAt,
                odometer = last?.odometer ?: vehicle.initialOdometer,
            )

        val forecast =
            forecast(
                rule = rule,
                lastServiceDate = since.date,
                lastServiceOdometer = since.odometer,
                history = history,
                zone = zone,
            ) ?: return null

        return ServiceDue(rule, forecast, since)
    }

    fun nextDue(
        rules: List<ServiceRule>,
        vehicle: Vehicle,
        services: List<ServiceEntry>,
        history: List<OdometerSample>,
        zone: ZoneId = ZoneId.systemDefault(),
    ): ServiceDue? =
        rules
            .mapNotNull { due(it, vehicle, services, history, zone) }
            .minWithOrNull(byDueDate<ServiceDue> { it.forecast.dueDate }.thenBy { it.rule.kind.rawValue })

    fun status(
        due: ServiceDue,
        currentOdometer: Double,
        now: Instant,
        zone: ZoneId = ZoneId.systemDefault(),
    ): MaintenanceStatus {
        val today = now.startOfDay(zone)
        val start = due.since.date.startOfDay(zone)

        var remainingDays: Int? = null
        var dateProgress = 0.0
        var isDueSoon = false
        val dueDate = due.forecast.dueDate
        if (dueDate != null) {
            val estimated = dueDate.startOfDay(zone)
            val remaining = daysBetween(today, estimated, zone)
            val periodDays = maxOf(1, daysBetween(start, estimated, zone))
            remainingDays = remaining
            dateProgress = (periodDays - remaining).toDouble() / periodDays
            isDueSoon = remaining <= window(periodDays.toDouble(), dueSoonDays)
        }

        var overdueBy: MaintenanceForecast.Trigger? = null
        var calendarDueDate: Instant? = null
        val months = due.rule.monthInterval
        if (months != null && months > 0) {
            calendarDueDate = start.plusMonths(months.toLong(), zone)
            if (calendarDueDate < today) overdueBy = MaintenanceForecast.Trigger.CALENDAR
        }

        var remainingKilometers: Double? = null
        var distanceProgress = 0.0
        val interval = due.rule.kilometerInterval
        if (interval != null && interval > 0) {
            val remaining = due.since.odometer + interval - currentOdometer
            remainingKilometers = remaining
            distanceProgress = (interval - remaining) / interval
            if (remaining <= 0 && overdueBy == null) overdueBy = MaintenanceForecast.Trigger.DISTANCE
            isDueSoon = isDueSoon || remaining <= window(interval, dueSoonKilometers)
        }

        val level =
            when {
                overdueBy != null -> MaintenanceStatus.Level.OVERDUE
                isDueSoon -> MaintenanceStatus.Level.DUE_SOON
                else -> MaintenanceStatus.Level.ON_TRACK
            }

        return MaintenanceStatus(
            level = level,
            progress = minOf(1.0, maxOf(0.0, distanceProgress, dateProgress)),
            remainingKilometers = remainingKilometers,
            remainingDays = remainingDays,
            overdueBy = overdueBy,
            calendarDueDate = calendarDueDate,
        )
    }

    fun <T> byDueDate(dueDate: (T) -> Instant?): Comparator<T> = compareBy(nullsLast()) { dueDate(it) }

    private fun window(
        length: Double,
        range: ClosedFloatingPointRange<Double>,
    ): Double = minOf(range.endInclusive, maxOf(range.start, length * DUE_SOON_FRACTION))

    private fun roundedAwayFromZero(value: Double): Long = if (value >= 0) floor(value + 0.5).toLong() else -floor(-value + 0.5).toLong()
}
