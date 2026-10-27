package br.com.joaoduartemariucio.kmais_app_android.data.repositories

import br.com.joaoduartemariucio.kmais_app_android.data.mappers.VehicleMapper
import br.com.joaoduartemariucio.kmais_app_android.data.models.VehicleDao
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.Vehicle
import br.com.joaoduartemariucio.kmais_app_android.domain.errors.RepositoryError
import br.com.joaoduartemariucio.kmais_app_android.domain.repositories.VehicleRepository
import java.util.UUID

class RoomVehicleRepository(
    private val dao: VehicleDao,
) : VehicleRepository {
    override suspend fun all(includingArchived: Boolean): List<Vehicle> =
        persisting { (if (includingArchived) dao.all() else dao.active()).map(VehicleMapper::entity) }

    override suspend fun vehicle(id: UUID): Vehicle? = persisting { dao.byId(id)?.let(VehicleMapper::entity) }

    override suspend fun save(vehicle: Vehicle) =
        persisting {
            val createdAt = dao.byId(vehicle.id)?.createdAt ?: vehicle.createdAt
            dao.upsert(VehicleMapper.model(vehicle.copy(createdAt = createdAt)))
        }

    override suspend fun setArchived(
        isArchived: Boolean,
        vehicleId: UUID,
    ) = persisting {
        if (dao.setArchived(vehicleId, isArchived) == 0) throw RepositoryError.VehicleNotFound(vehicleId)
    }
}
