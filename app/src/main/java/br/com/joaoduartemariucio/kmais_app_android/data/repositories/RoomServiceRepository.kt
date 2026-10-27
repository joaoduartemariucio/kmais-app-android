package br.com.joaoduartemariucio.kmais_app_android.data.repositories

import br.com.joaoduartemariucio.kmais_app_android.data.mappers.ServiceMapper
import br.com.joaoduartemariucio.kmais_app_android.data.models.ServiceDao
import br.com.joaoduartemariucio.kmais_app_android.data.models.VehicleDao
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.ServiceEntry
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.ServiceRule
import br.com.joaoduartemariucio.kmais_app_android.domain.errors.RepositoryError
import br.com.joaoduartemariucio.kmais_app_android.domain.repositories.ServiceRepository
import java.util.UUID

class RoomServiceRepository(
    private val dao: ServiceDao,
    private val vehicles: VehicleDao,
) : ServiceRepository {
    override suspend fun entries(vehicleId: UUID): List<ServiceEntry> = persisting { dao.entries(vehicleId).map(ServiceMapper::entity) }

    override suspend fun rules(vehicleId: UUID): List<ServiceRule> = persisting { dao.rules(vehicleId).map(ServiceMapper::entity) }

    override suspend fun save(entry: ServiceEntry) =
        persisting {
            val existing = dao.entryById(entry.id)
            if (existing == null && vehicles.byId(entry.vehicleId) == null) {
                throw RepositoryError.VehicleNotFound(entry.vehicleId)
            }
            dao.upsert(ServiceMapper.model(entry.copy(vehicleId = existing?.vehicleId ?: entry.vehicleId)))
        }

    override suspend fun save(
        rules: List<ServiceRule>,
        vehicleId: UUID,
    ) = persisting {
        if (vehicles.byId(vehicleId) == null) throw RepositoryError.VehicleNotFound(vehicleId)
        dao.replaceRules(vehicleId, rules.map { ServiceMapper.model(it.copy(vehicleId = vehicleId)) })
    }

    override suspend fun deleteEntry(id: UUID) =
        persisting {
            if (dao.deleteEntry(id) == 0) throw RepositoryError.EntryNotFound(id)
        }

    override suspend fun deleteRule(id: UUID) =
        persisting {
            if (dao.deleteRule(id) == 0) throw RepositoryError.EntryNotFound(id)
        }
}
