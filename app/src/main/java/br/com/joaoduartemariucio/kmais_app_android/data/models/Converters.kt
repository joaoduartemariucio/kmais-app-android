package br.com.joaoduartemariucio.kmais_app_android.data.models

import androidx.room.TypeConverter
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.ServiceKind
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.VehicleKind
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class Converters {
    @TypeConverter
    fun uuid(value: String): UUID = UUID.fromString(value)

    @TypeConverter
    fun uuidText(value: UUID): String = value.toString()

    @TypeConverter
    fun instant(value: Long): Instant = Instant.ofEpochMilli(value)

    @TypeConverter
    fun instantMillis(value: Instant): Long = value.toEpochMilli()

    @TypeConverter
    fun decimal(value: String): BigDecimal = BigDecimal(value)

    @TypeConverter
    fun decimalText(value: BigDecimal): String = value.toPlainString()

    @TypeConverter
    fun serviceKind(value: String): ServiceKind = ServiceKind.entries.firstOrNull { it.rawValue == value } ?: ServiceKind.OTHER

    @TypeConverter
    fun serviceKindText(value: ServiceKind): String = value.rawValue

    @TypeConverter
    fun vehicleKind(value: String): VehicleKind = VehicleKind.entries.firstOrNull { it.name.lowercase() == value } ?: VehicleKind.CAR

    @TypeConverter
    fun vehicleKindText(value: VehicleKind): String = value.name.lowercase()
}
