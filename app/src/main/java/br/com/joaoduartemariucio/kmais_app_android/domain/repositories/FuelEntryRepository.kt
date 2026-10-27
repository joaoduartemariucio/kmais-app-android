package br.com.joaoduartemariucio.kmais_app_android.domain.repositories

import br.com.joaoduartemariucio.kmais_app_android.domain.entities.FuelEntry
import java.util.UUID

interface FuelEntryRepository {
    suspend fun entries(vehicleId: UUID): List<FuelEntry>

    suspend fun save(entry: FuelEntry)

    suspend fun delete(id: UUID)
}
