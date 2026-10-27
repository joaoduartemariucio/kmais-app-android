package br.com.joaoduartemariucio.kmais_app_android.calculations

import br.com.joaoduartemariucio.kmais_app_android.domain.calculations.Consumption
import br.com.joaoduartemariucio.kmais_app_android.domain.calculations.ConsumptionAnomaly.Kind
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.FuelEntry
import org.junit.Test
import java.util.UUID
import kotlin.test.assertEquals

class ConsumptionAnomalyTests {
    private val first = UUID.fromString("00000000-0000-0000-0000-000000000001")

    private val scenarios: List<Triple<String, List<FuelEntry>, List<Kind>>> =
        listOf(
            Triple("empty history", emptyList(), emptyList()),
            Triple(
                "a consistent history yields no anomaly",
                listOf(full(0, 1_000.0, 40.0), partial(5, 1_200.0, 20.0), full(10, 1_400.0, 20.0)),
                emptyList(),
            ),
            Triple(
                "a backwards odometer",
                listOf(full(0, 1_000.0, 40.0), full(10, 900.0, 40.0)),
                listOf(Kind.OdometerWentBackwards(1_000.0)),
            ),
            Triple(
                "one isolated mistake does not taint the records that follow",
                listOf(full(0, 1_000.0, 40.0), full(10, 900.0, 40.0), full(20, 1_100.0, 40.0)),
                listOf(Kind.OdometerWentBackwards(1_000.0)),
            ),
            Triple("zero liters", listOf(full(0, 1_000.0, 40.0), full(10, 1_400.0, 0.0)), listOf(Kind.NonPositiveLiters)),
            Triple("negative liters", listOf(full(0, 1_000.0, 40.0), full(10, 1_400.0, -5.0)), listOf(Kind.NonPositiveLiters)),
            Triple(
                "an unchanged odometer with different data",
                listOf(full(0, 1_000.0, 40.0), full(10, 1_000.0, 30.0)),
                listOf(Kind.OdometerUnchanged(1_000.0)),
            ),
            Triple(
                "an identical duplicate from import or sync",
                listOf(full(0, 1_000.0, 40.0, id = first), full(0, 1_000.0, 40.0)),
                listOf(Kind.Duplicate(first)),
            ),
            Triple(
                "one record can accumulate more than one anomaly",
                listOf(full(0, 1_000.0, 40.0), full(10, 900.0, 0.0)),
                listOf(Kind.NonPositiveLiters, Kind.OdometerWentBackwards(1_000.0)),
            ),
        )

    @Test
    fun `anomalies per scenario`() {
        for ((name, records, expected) in scenarios) {
            assertEquals(expected, Consumption.anomalies(records).map { it.kind }, name)
        }
    }

    @Test
    fun `the anomaly points at the record that caused it`() {
        val anomalies = Consumption.anomalies(listOf(full(0, 1_000.0, 40.0), full(10, 900.0, 40.0, id = first)))

        assertEquals(1, anomalies.size)
        assertEquals(first, anomalies.first().record.id)
    }

    @Test
    fun `anomaly detection and calculation are independent passes`() {
        val records = listOf(full(0, 1_000.0, 40.0), full(10, 900.0, 40.0), full(20, 1_400.0, 50.0))

        assertEquals(1, Consumption.anomalies(records).size)
        assertEquals(1, Consumption.intervals(records).size)
    }
}
