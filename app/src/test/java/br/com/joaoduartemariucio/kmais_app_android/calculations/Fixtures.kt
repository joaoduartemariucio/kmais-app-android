package br.com.joaoduartemariucio.kmais_app_android.calculations

import br.com.joaoduartemariucio.kmais_app_android.domain.entities.FuelEntry
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.OdometerSample
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.ServiceEntry
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.ServiceKind
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.ServiceRule
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

private val referenceDate: Instant = Instant.parse("2001-01-01T00:00:00Z")

fun day(offset: Int): Instant = referenceDate.plusSeconds(offset * 86_400L)

fun hour(
    hour: Int,
    dayOffset: Int,
): Instant = day(dayOffset).plusSeconds(hour * 3_600L)

val utc: ZoneId = ZoneOffset.UTC

val testVehicleId: UUID = UUID.fromString("00000000-0000-0000-0000-00000000CA11")

fun decimal(value: String): BigDecimal = BigDecimal(value)

fun assertDecimal(
    expected: String,
    actual: BigDecimal?,
    message: String? = null,
) {
    assertNotNull(actual, message)
    assertEquals(0, BigDecimal(expected).compareTo(actual), message ?: "expected $expected, got $actual")
}

fun full(
    dayOffset: Int,
    odometer: Double,
    liters: Double,
    price: BigDecimal = BigDecimal(5),
    id: UUID = UUID.randomUUID(),
): FuelEntry =
    FuelEntry(
        id = id,
        vehicleId = testVehicleId,
        date = day(dayOffset),
        odometer = odometer,
        liters = liters,
        pricePerLiter = price,
        isFullTank = true,
    )

fun partial(
    dayOffset: Int,
    odometer: Double,
    liters: Double,
    price: BigDecimal = BigDecimal(5),
    id: UUID = UUID.randomUUID(),
): FuelEntry = full(dayOffset, odometer, liters, price, id).copy(isFullTank = false)

fun service(
    dayOffset: Int,
    odometer: Double,
    cost: BigDecimal,
): ServiceEntry = ServiceEntry(vehicleId = testVehicleId, date = day(dayOffset), odometer = odometer, cost = cost)

fun sample(
    dayOffset: Int,
    odometer: Double,
): OdometerSample = OdometerSample(day(dayOffset), odometer)

fun rule(
    kilometers: Double?,
    months: Int?,
): ServiceRule = ServiceRule(vehicleId = testVehicleId, kind = ServiceKind.OIL_CHANGE, kilometerInterval = kilometers, monthInterval = months)
