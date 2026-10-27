package br.com.joaoduartemariucio.kmais_app_android.domain.entities

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class ServiceEntry(
    val id: UUID = UUID.randomUUID(),
    val vehicleId: UUID,
    val kind: ServiceKind = ServiceKind.OTHER,
    override val date: Instant,
    override val odometer: Double,
    val cost: BigDecimal,
    val notes: String = "",
) : Chronological
