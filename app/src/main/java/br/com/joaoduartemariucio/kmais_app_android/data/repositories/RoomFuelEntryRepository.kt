package br.com.joaoduartemariucio.kmais_app_android.data.repositories

import br.com.joaoduartemariucio.kmais_app_android.data.mappers.FuelEntryMapper
import br.com.joaoduartemariucio.kmais_app_android.data.models.FuelEntryDao
import br.com.joaoduartemariucio.kmais_app_android.data.models.VehicleDao
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.FuelEntry
import br.com.joaoduartemariucio.kmais_app_android.domain.errors.RepositoryError
import br.com.joaoduartemariucio.kmais_app_android.domain.repositories.FuelEntryRepository
import java.util.UUID

class RoomFuelEntryRepository(
    private val dao: FuelEntryDao,
    private val vehicles: VehicleDao,
) : FuelEntryRepository {
    override suspend fun entries(vehicleId: UUID): List<FuelEntry> = persisting { dao.entries(vehicleId).map(FuelEntryMapper::entity) }

    override suspend fun save(entry: FuelEntry) =
        persisting {
            val existing = dao.byId(entry.id)
            if (existing == null && vehicles.byId(entry.vehicleId) == null) {
                throw RepositoryError.VehicleNotFound(entry.vehicleId)
            }
            dao.upsert(FuelEntryMapper.model(entry.copy(vehicleId = existing?.vehicleId ?: entry.vehicleId)))
        }

    override suspend fun delete(id: UUID) =
        persisting {
            if (dao.delete(id) == 0) throw RepositoryError.EntryNotFound(id)
        }
}
