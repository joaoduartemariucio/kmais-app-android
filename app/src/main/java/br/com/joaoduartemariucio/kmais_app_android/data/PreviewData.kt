package br.com.joaoduartemariucio.kmais_app_android.data

import br.com.joaoduartemariucio.kmais_app_android.data.mappers.FuelEntryMapper
import br.com.joaoduartemariucio.kmais_app_android.data.mappers.ServiceMapper
import br.com.joaoduartemariucio.kmais_app_android.data.mappers.VehicleMapper
import br.com.joaoduartemariucio.kmais_app_android.data.models.KmaisDatabase
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.FuelEntry
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.ServiceEntry
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.ServiceKind
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.ServiceRule
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.Vehicle
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.VehicleKind
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

object PreviewData {
    private val referenceNow: Instant = Instant.now()

    val car =
        Vehicle(
            id = UUID.fromString("00000000-0000-0000-0000-0000000000C1"),
            name = "Gol 1.6",
            kind = VehicleKind.CAR,
            plate = "ABC1D23",
            initialOdometer = 38_000.0,
            tankCapacity = 55.0,
            createdAt = date(monthsAgo = 5),
        )

    val motorcycle =
        Vehicle(
            id = UUID.fromString("00000000-0000-0000-0000-0000000000B1"),
            name = "CB 500",
            kind = VehicleKind.MOTORCYCLE,
            plate = "XYZ9K87",
            initialOdometer = 12_000.0,
            tankCapacity = 17.0,
            createdAt = date(monthsAgo = 4),
        )

    suspend fun seed(database: KmaisDatabase) {
        if (database.vehicles().byId(car.id) != null) return
        seedCar(database)
        seedMotorcycle(database)
    }

    private fun date(
        monthsAgo: Long,
        plusDays: Long = 0,
    ): Instant =
        referenceNow
            .atZone(ZoneId.systemDefault())
            .minusMonths(monthsAgo)
            .plusDays(plusDays)
            .toInstant()

    private suspend fun fill(
        database: KmaisDatabase,
        vehicle: Vehicle,
        monthsAgo: Long,
        days: Long,
        odometer: Double,
        liters: Double,
        price: String,
        full: Boolean,
    ) = database.fuelEntries().upsert(
        FuelEntryMapper.model(
            FuelEntry(
                vehicleId = vehicle.id,
                date = date(monthsAgo, days),
                odometer = odometer,
                liters = liters,
                pricePerLiter = BigDecimal(price),
                isFullTank = full,
            ),
        ),
    )

    private suspend fun seedCar(database: KmaisDatabase) {
        database.vehicles().upsert(VehicleMapper.model(car))

        var odometer = car.initialOdometer
        for (month in 4L downTo 0L) {
            odometer += 620
            fill(database, car, month, -27, odometer, 42.0, "5.89", full = true)
            odometer += 180
            fill(database, car, month, -13, odometer, 15.0, "5.95", full = false)
        }

        database.services().upsert(
            ServiceMapper.model(
                ServiceEntry(
                    vehicleId = car.id,
                    kind = ServiceKind.OIL_CHANGE,
                    date = date(monthsAgo = 3),
                    odometer = car.initialOdometer + 1_500,
                    cost = BigDecimal("320.00"),
                    notes = "Óleo sintético 5W30",
                ),
            ),
        )
        database.services().upsertRules(
            listOf(
                ServiceMapper.model(
                    ServiceRule(vehicleId = car.id, kind = ServiceKind.OIL_CHANGE, kilometerInterval = 10_000.0, monthInterval = 12),
                ),
            ),
        )
    }

    private suspend fun seedMotorcycle(database: KmaisDatabase) {
        database.vehicles().upsert(VehicleMapper.model(motorcycle))

        var odometer = motorcycle.initialOdometer
        for (month in 3L downTo 0L) {
            odometer += 340
            fill(database, motorcycle, month, -22, odometer, 13.0, "5.79", full = true)
        }

        database.services().upsertRules(
            listOf(ServiceMapper.model(ServiceRule(vehicleId = motorcycle.id, kind = ServiceKind.CHAIN, kilometerInterval = 1_000.0))),
        )
    }
}
