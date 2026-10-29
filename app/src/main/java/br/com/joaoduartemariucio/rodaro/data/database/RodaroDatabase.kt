package br.com.joaoduartemariucio.rodaro.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import br.com.joaoduartemariucio.rodaro.data.fueling.models.FuelEntryDao
import br.com.joaoduartemariucio.rodaro.data.fueling.models.FuelEntryModel
import br.com.joaoduartemariucio.rodaro.data.maintenance.models.ServiceDao
import br.com.joaoduartemariucio.rodaro.data.maintenance.models.ServiceEntryModel
import br.com.joaoduartemariucio.rodaro.data.maintenance.models.ServiceRuleModel
import br.com.joaoduartemariucio.rodaro.data.trips.models.TripRouteDao
import br.com.joaoduartemariucio.rodaro.data.trips.models.TripRouteModel
import br.com.joaoduartemariucio.rodaro.data.vehicles.models.VehicleDao
import br.com.joaoduartemariucio.rodaro.data.vehicles.models.VehicleModel

@Database(
    entities = [
        VehicleModel::class,
        FuelEntryModel::class,
        ServiceEntryModel::class,
        ServiceRuleModel::class,
        TripRouteModel::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class RodaroDatabase : RoomDatabase() {
    abstract fun vehicles(): VehicleDao

    abstract fun fuelEntries(): FuelEntryDao

    abstract fun services(): ServiceDao

    abstract fun tripRoutes(): TripRouteDao

    companion object {
        private const val FILE_NAME = "rodaro.db"

        internal val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `trip_routes` (
                            `id` TEXT NOT NULL,
                            `vehicleId` TEXT NOT NULL,
                            `origin` TEXT NOT NULL,
                            `destination` TEXT NOT NULL,
                            `distanceKm` REAL NOT NULL,
                            `isRoundTrip` INTEGER NOT NULL,
                            `hasTolls` INTEGER NOT NULL,
                            `liters` REAL NOT NULL,
                            `fuelCost` TEXT NOT NULL,
                            `costPerKilometer` TEXT NOT NULL,
                            `createdAt` INTEGER NOT NULL,
                            PRIMARY KEY(`id`)
                        )
                        """.trimIndent(),
                    )
                }
            }

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
            return builder.addMigrations(MIGRATION_1_2).build()
        }
    }
}
