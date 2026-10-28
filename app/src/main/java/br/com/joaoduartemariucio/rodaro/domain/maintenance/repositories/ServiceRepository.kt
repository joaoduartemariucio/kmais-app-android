package br.com.joaoduartemariucio.rodaro.domain.maintenance.repositories

import br.com.joaoduartemariucio.rodaro.domain.maintenance.entities.ServiceEntry
import br.com.joaoduartemariucio.rodaro.domain.maintenance.entities.ServiceRule
import java.util.UUID

interface ServiceRepository {
    suspend fun entries(vehicleId: UUID): List<ServiceEntry>

    suspend fun rules(vehicleId: UUID): List<ServiceRule>

    suspend fun save(entry: ServiceEntry)

    suspend fun save(
        rules: List<ServiceRule>,
        vehicleId: UUID,
    )

    suspend fun deleteEntry(id: UUID)

    suspend fun deleteRule(id: UUID)
}
