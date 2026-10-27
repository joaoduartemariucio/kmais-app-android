package br.com.joaoduartemariucio.kmais_app_android.domain.entities

import java.util.UUID

data class ServiceRule(
    val id: UUID = UUID.randomUUID(),
    val vehicleId: UUID,
    val kind: ServiceKind = ServiceKind.OTHER,
    val kilometerInterval: Double? = null,
    val monthInterval: Int? = null,
)
