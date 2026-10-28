package br.com.joaoduartemariucio.rodaro.domain.maintenance.entities

import java.util.UUID

data class ServiceRule(
    val id: UUID = UUID.randomUUID(),
    val vehicleId: UUID,
    val kind: ServiceKind = ServiceKind.OTHER,
    val kilometerInterval: Double? = null,
    val monthInterval: Int? = null,
)
