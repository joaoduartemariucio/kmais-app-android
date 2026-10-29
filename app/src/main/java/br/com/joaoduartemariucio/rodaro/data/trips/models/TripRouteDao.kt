package br.com.joaoduartemariucio.rodaro.data.trips.models

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import java.util.UUID

@Dao
interface TripRouteDao {
    @Query("SELECT * FROM trip_routes ORDER BY createdAt DESC")
    suspend fun all(): List<TripRouteModel>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(model: TripRouteModel)

    @Query("DELETE FROM trip_routes WHERE id = :id")
    suspend fun delete(id: UUID)
}
