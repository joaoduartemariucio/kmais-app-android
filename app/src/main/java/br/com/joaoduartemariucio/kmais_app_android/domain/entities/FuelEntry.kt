package br.com.joaoduartemariucio.kmais_app_android.domain.entities

import br.com.joaoduartemariucio.kmais_app_android.domain.calculations.toDecimal
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class FuelEntry(
    val id: UUID = UUID.randomUUID(),
    val vehicleId: UUID,
    override val date: Instant,
    override val odometer: Double,
    val liters: Double,
    val pricePerLiter: BigDecimal,
    val isFullTank: Boolean,
) : Chronological {
    val totalCost: BigDecimal get() = liters.toDecimal().multiply(pricePerLiter)
}
