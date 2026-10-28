package br.com.joaoduartemariucio.rodaro.data.maintenance.models

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import java.util.UUID

@Dao
interface ServiceDao {
    @Query("SELECT * FROM service_entries WHERE vehicleId = :vehicleId ORDER BY date, odometer")
    suspend fun entries(vehicleId: UUID): List<ServiceEntryModel>

    @Query("SELECT * FROM service_rules WHERE vehicleId = :vehicleId")
    suspend fun rules(vehicleId: UUID): List<ServiceRuleModel>

    @Query("SELECT * FROM service_entries WHERE id = :id")
    suspend fun entryById(id: UUID): ServiceEntryModel?

    @Upsert
    suspend fun upsert(model: ServiceEntryModel)

    @Upsert
    suspend fun upsertRules(models: List<ServiceRuleModel>)

    @Query("DELETE FROM service_rules WHERE vehicleId = :vehicleId AND id NOT IN (:keptIds)")
    suspend fun deleteRules(
        vehicleId: UUID,
        keptIds: List<UUID>,
    )

    @Transaction
    suspend fun replaceRules(
        vehicleId: UUID,
        models: List<ServiceRuleModel>,
    ) {
        deleteRules(vehicleId, models.map { it.id })
        upsertRules(models)
    }

    @Query("DELETE FROM service_entries WHERE id = :id")
    suspend fun deleteEntry(id: UUID): Int

    @Query("DELETE FROM service_rules WHERE id = :id")
    suspend fun deleteRule(id: UUID): Int
}
