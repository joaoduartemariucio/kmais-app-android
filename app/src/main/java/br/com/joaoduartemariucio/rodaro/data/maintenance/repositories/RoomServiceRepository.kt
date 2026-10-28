package br.com.joaoduartemariucio.rodaro.data.maintenance.repositories

import br.com.joaoduartemariucio.rodaro.data.common.repositories.persisting
import br.com.joaoduartemariucio.rodaro.data.maintenance.mappers.ServiceMapper
import br.com.joaoduartemariucio.rodaro.data.maintenance.models.ServiceDao
import br.com.joaoduartemariucio.rodaro.data.vehicles.models.VehicleDao
import br.com.joaoduartemariucio.rodaro.domain.common.errors.RepositoryError
import br.com.joaoduartemariucio.rodaro.domain.maintenance.entities.ServiceEntry
import br.com.joaoduartemariucio.rodaro.domain.maintenance.entities.ServiceRule
import br.com.joaoduartemariucio.rodaro.domain.maintenance.repositories.ServiceRepository
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
