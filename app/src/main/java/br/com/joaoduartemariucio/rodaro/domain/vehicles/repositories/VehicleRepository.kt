package br.com.joaoduartemariucio.rodaro.domain.vehicles.repositories

import br.com.joaoduartemariucio.rodaro.domain.vehicles.entities.Vehicle
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
