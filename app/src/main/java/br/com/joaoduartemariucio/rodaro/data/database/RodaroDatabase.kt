package br.com.joaoduartemariucio.rodaro.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import br.com.joaoduartemariucio.rodaro.data.fueling.models.FuelEntryDao
import br.com.joaoduartemariucio.rodaro.data.fueling.models.FuelEntryModel
import br.com.joaoduartemariucio.rodaro.data.maintenance.models.ServiceDao
import br.com.joaoduartemariucio.rodaro.data.maintenance.models.ServiceEntryModel
import br.com.joaoduartemariucio.rodaro.data.maintenance.models.ServiceRuleModel
import br.com.joaoduartemariucio.rodaro.data.vehicles.models.VehicleDao
import br.com.joaoduartemariucio.rodaro.data.vehicles.models.VehicleModel

@Database(
    entities = [VehicleModel::class, FuelEntryModel::class, ServiceEntryModel::class, ServiceRuleModel::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class RodaroDatabase : RoomDatabase() {
    abstract fun vehicles(): VehicleDao

    abstract fun fuelEntries(): FuelEntryDao

    abstract fun services(): ServiceDao

    companion object {
        private const val FILE_NAME = "rodaro.db"

        fun create(
            context: Context,
            inMemory: Boolean = false,
        ): RodaroDatabase {
            val builder =
                if (inMemory) {
                    Room.inMemoryDatabaseBuilder(context, RodaroDatabase::class.java)
                } else {
                    Room.databaseBuilder(context, RodaroDatabase::class.java, FILE_NAME)
                }
            return builder.build()
        }
    }
}
