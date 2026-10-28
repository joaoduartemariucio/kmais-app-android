package br.com.joaoduartemariucio.rodaro.domain.fueling.calculations

import br.com.joaoduartemariucio.rodaro.domain.common.entities.sortedChronologically
import br.com.joaoduartemariucio.rodaro.domain.fueling.entities.FuelEntry

object Consumption {
    fun intervals(records: List<FuelEntry>): List<ConsumptionInterval> {
        val intervals = mutableListOf<ConsumptionInterval>()
        var open: FuelEntry? = null
        var litersSinceOpen = 0.0

        for (record in records.sortedChronologically().filter { it.liters > 0 }) {
            val start = open
            if (start == null) {
                if (record.isFullTank) open = record
                continue
            }

            litersSinceOpen += record.liters
            if (!record.isFullTank) continue

            val interval =
                ConsumptionInterval(
                    startDate = start.date,
                    endDate = record.date,
                    startOdometer = start.odometer,
                    endOdometer = record.odometer,
                    liters = litersSinceOpen,
                )
            if (interval.distance > 0) {
                intervals += interval
            }

            open = record
            litersSinceOpen = 0.0
        }

        return intervals
    }

    fun latest(records: List<FuelEntry>): ConsumptionInterval? = intervals(records).lastOrNull()

    fun average(records: List<FuelEntry>): Double? {
        val intervals = intervals(records)
        if (intervals.isEmpty()) return null

        val liters = intervals.sumOf { it.liters }
        if (liters <= 0) return null

        return intervals.sumOf { it.distance } / liters
    }

    fun anomalies(records: List<FuelEntry>): List<ConsumptionAnomaly> {
        val anomalies = mutableListOf<ConsumptionAnomaly>()
        var previous: FuelEntry? = null

        for (record in records.sortedChronologically()) {
            if (record.liters <= 0) {
                anomalies += ConsumptionAnomaly(record, ConsumptionAnomaly.Kind.NonPositiveLiters)
            }

            val last = previous
            previous = record
            if (last == null) continue

            if (record.odometer < last.odometer) {
                anomalies += ConsumptionAnomaly(record, ConsumptionAnomaly.Kind.OdometerWentBackwards(last.odometer))
            } else if (record.odometer == last.odometer) {
                val isDuplicate =
                    record.date == last.date &&
                        record.liters == last.liters &&
                        record.pricePerLiter.compareTo(last.pricePerLiter) == 0
                anomalies +=
                    ConsumptionAnomaly(
                        record,
                        if (isDuplicate) {
                            ConsumptionAnomaly.Kind.Duplicate(last.id)
                        } else {
                            ConsumptionAnomaly.Kind.OdometerUnchanged(last.odometer)
                        },
                    )
            }
        }

        return anomalies
    }
}
