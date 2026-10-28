package br.com.joaoduartemariucio.rodaro.domain.vehicles.entities

import br.com.joaoduartemariucio.rodaro.domain.common.entities.Chronological
import java.time.Instant

data class OdometerSample(
    override val date: Instant,
    override val odometer: Double,
) : Chronological
