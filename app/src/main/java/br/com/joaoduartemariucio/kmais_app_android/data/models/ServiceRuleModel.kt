package br.com.joaoduartemariucio.kmais_app_android.data.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.ServiceKind
import java.util.UUID

@Entity(
    tableName = "service_rules",
    foreignKeys = [
        ForeignKey(VehicleModel::class, ["id"], ["vehicleId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("vehicleId")],
)
data class ServiceRuleModel(
    @PrimaryKey val id: UUID,
    val vehicleId: UUID,
    val kind: ServiceKind,
    val kilometerInterval: Double?,
    val monthInterval: Int?,
)
