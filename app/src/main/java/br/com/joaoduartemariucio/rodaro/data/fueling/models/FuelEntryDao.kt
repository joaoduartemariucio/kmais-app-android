package br.com.joaoduartemariucio.rodaro.data.fueling.models

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import java.util.UUID

@Dao
interface FuelEntryDao {
    @Query("SELECT * FROM fuel_entries WHERE vehicleId = :vehicleId ORDER BY date, odometer")
    suspend fun entries(vehicleId: UUID): List<FuelEntryModel>

    @Query("SELECT * FROM fuel_entries WHERE id = :id")
    suspend fun byId(id: UUID): FuelEntryModel?

    @Upsert
    suspend fun upsert(model: FuelEntryModel)

    @Query("DELETE FROM fuel_entries WHERE id = :id")
    suspend fun delete(id: UUID): Int
}
