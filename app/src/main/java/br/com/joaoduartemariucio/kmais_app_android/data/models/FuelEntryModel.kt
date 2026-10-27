package br.com.joaoduartemariucio.kmais_app_android.data.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity(
    tableName = "fuel_entries",
    foreignKeys = [
        ForeignKey(VehicleModel::class, ["id"], ["vehicleId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("vehicleId")],
)
data class FuelEntryModel(
    @PrimaryKey val id: UUID,
    val vehicleId: UUID,
    val date: Instant,
    val odometer: Double,
    val liters: Double,
    val pricePerLiter: BigDecimal,
    val isFullTank: Boolean,
)
