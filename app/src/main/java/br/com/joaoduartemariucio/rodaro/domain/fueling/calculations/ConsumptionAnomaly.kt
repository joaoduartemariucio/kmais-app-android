package br.com.joaoduartemariucio.rodaro.domain.fueling.calculations

import br.com.joaoduartemariucio.rodaro.domain.fueling.entities.FuelEntry
import java.util.UUID

data class ConsumptionAnomaly(
    val record: FuelEntry,
    val kind: Kind,
) {
    sealed interface Kind {
        data class OdometerWentBackwards(
            val previous: Double,
        ) : Kind

        data class OdometerUnchanged(
            val previous: Double,
        ) : Kind

        data object NonPositiveLiters : Kind

        data class Duplicate(
            val of: UUID,
        ) : Kind
    }
}
