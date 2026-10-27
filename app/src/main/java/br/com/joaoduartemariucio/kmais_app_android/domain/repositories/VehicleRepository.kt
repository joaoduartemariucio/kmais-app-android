package br.com.joaoduartemariucio.kmais_app_android.domain.repositories

import br.com.joaoduartemariucio.kmais_app_android.domain.entities.Vehicle
import java.util.UUID

interface VehicleRepository {
    suspend fun all(includingArchived: Boolean): List<Vehicle>

    suspend fun vehicle(id: UUID): Vehicle?

    suspend fun save(vehicle: Vehicle)

    suspend fun setArchived(
        isArchived: Boolean,
        vehicleId: UUID,
    )
}
