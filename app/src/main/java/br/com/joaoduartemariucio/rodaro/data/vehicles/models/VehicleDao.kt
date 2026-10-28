package br.com.joaoduartemariucio.rodaro.data.vehicles.models

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import java.util.UUID

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles ORDER BY createdAt DESC")
    suspend fun all(): List<VehicleModel>

    @Query("SELECT * FROM vehicles WHERE isArchived = 0 ORDER BY createdAt DESC")
    suspend fun active(): List<VehicleModel>

    @Query("SELECT * FROM vehicles WHERE id = :id")
    suspend fun byId(id: UUID): VehicleModel?

    @Upsert
    suspend fun upsert(model: VehicleModel)

    @Query("UPDATE vehicles SET isArchived = :isArchived WHERE id = :id")
    suspend fun setArchived(
        id: UUID,
        isArchived: Boolean,
    ): Int
}
