package br.com.joaoduartemariucio.kmais_app_android.domain.entities

import java.time.Instant

data class OdometerSample(
    override val date: Instant,
    override val odometer: Double,
) : Chronological
