package br.com.joaoduartemariucio.kmais_app_android.domain.entities

import java.time.Instant
import java.util.UUID

data class Vehicle(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val kind: VehicleKind = VehicleKind.CAR,
    val plate: String? = null,
    val initialOdometer: Double = 0.0,
    val tankCapacity: Double? = null,
    val createdAt: Instant,
    val isArchived: Boolean = false,
)
