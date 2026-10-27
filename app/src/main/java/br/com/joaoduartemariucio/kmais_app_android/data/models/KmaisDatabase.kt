package br.com.joaoduartemariucio.kmais_app_android.data.models

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [VehicleModel::class, FuelEntryModel::class, ServiceEntryModel::class, ServiceRuleModel::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class KmaisDatabase : RoomDatabase() {
    abstract fun vehicles(): VehicleDao

    abstract fun fuelEntries(): FuelEntryDao

    abstract fun services(): ServiceDao

    companion object {
        private const val FILE_NAME = "kmais.db"

        fun create(
            context: Context,
            inMemory: Boolean = false,
        ): KmaisDatabase {
            val builder =
                if (inMemory) {
                    Room.inMemoryDatabaseBuilder(context, KmaisDatabase::class.java)
                } else {
                    Room.databaseBuilder(context, KmaisDatabase::class.java, FILE_NAME)
                }
            return builder.build()
        }
    }
}
