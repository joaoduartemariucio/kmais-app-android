package br.com.joaoduartemariucio.kmais_app_android.domain.repositories

import br.com.joaoduartemariucio.kmais_app_android.domain.entities.ServiceEntry
import br.com.joaoduartemariucio.kmais_app_android.domain.entities.ServiceRule
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
