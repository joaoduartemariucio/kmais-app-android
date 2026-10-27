package br.com.joaoduartemariucio.kmais_app_android.presentation.vehicles

import br.com.joaoduartemariucio.kmais_app_android.R
import br.com.joaoduartemariucio.kmais_app_android.domain.calculations.monthStart
import br.com.joaoduartemariucio.kmais_app_android.domain.calculations.plusMonths
import br.com.joaoduartemariucio.kmais_app_android.domain.calculations.sum
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.FuelEntry
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.ServiceEntry
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

object VehicleTimeline {
    sealed interface Item {
        val id: UUID
        val date: Instant
        val odometer: Double
        val cost: BigDecimal

        data class Fuel(
            val entry: FuelEntry,
        ) : Item {
            override val id get() = entry.id
            override val date get() = entry.date
            override val odometer get() = entry.odometer
            override val cost: BigDecimal get() = entry.totalCost
        }

        data class Service(
            val entry: ServiceEntry,
        ) : Item {
            override val id get() = entry.id
            override val date get() = entry.date
            override val odometer get() = entry.odometer
            override val cost: BigDecimal get() = entry.cost
        }

        fun matches(filter: Filter): Boolean =
            when (filter) {
                Filter.ALL -> true
                Filter.FUEL -> this is Fuel
                Filter.SERVICE -> this is Service
            }
    }

    enum class Filter(
        val label: Int,
    ) {
        ALL(R.string.filter_all),
        FUEL(R.string.fuel),
        SERVICE(R.string.services),
    }

    data class MonthSection(
        val month: Instant,
        val items: List<Item>,
    ) {
        val total: BigDecimal get() = items.map { it.cost }.sum()

        fun filtered(filter: Filter): MonthSection? {
            val kept = items.filter { it.matches(filter) }
            return if (kept.isEmpty()) null else MonthSection(month, kept)
        }
    }

    data class MonthSpending(
        val month: Instant,
        val fuel: BigDecimal,
        val service: BigDecimal,
    ) {
        val total: BigDecimal get() = fuel + service
    }

    fun sections(
        fuel: List<FuelEntry>,
        services: List<ServiceEntry>,
        zone: ZoneId,
    ): List<MonthSection> =
        (fuel.map(Item::Fuel) + services.map(Item::Service))
            .groupBy { it.date.monthStart(zone) }
            .map { (month, items) ->
                MonthSection(month, items.sortedWith(compareByDescending<Item> { it.date }.thenByDescending { it.odometer }))
            }.sortedByDescending { it.month }

    fun spending(
        sections: List<MonthSection>,
        months: Int,
        endingAt: Instant,
        zone: ZoneId,
    ): List<MonthSpending> {
        val byMonth = sections.associate { it.month to it.items }
        val current = endingAt.monthStart(zone)

        return (0 until maxOf(0, months)).reversed().map { offset ->
            val month = current.plusMonths(-offset.toLong(), zone)
            val items = byMonth[month].orEmpty()
            MonthSpending(
                month = month,
                fuel = items.filter { it.matches(Filter.FUEL) }.map { it.cost }.sum(),
                service = items.filter { it.matches(Filter.SERVICE) }.map { it.cost }.sum(),
            )
        }
    }
}
