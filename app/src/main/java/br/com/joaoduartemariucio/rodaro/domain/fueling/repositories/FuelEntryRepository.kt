package br.com.joaoduartemariucio.rodaro.domain.fueling.repositories

import br.com.joaoduartemariucio.rodaro.domain.fueling.entities.FuelEntry
import java.util.UUID

interface FuelEntryRepository {
    suspend fun entries(vehicleId: UUID): List<FuelEntry>

    suspend fun save(entry: FuelEntry)

    suspend fun delete(id: UUID)
}
