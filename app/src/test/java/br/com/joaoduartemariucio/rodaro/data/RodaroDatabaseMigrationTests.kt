package br.com.joaoduartemariucio.rodaro.data

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import br.com.joaoduartemariucio.rodaro.data.database.RodaroDatabase
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class RodaroDatabaseMigrationTests {
    @Test
    fun `migrating from 1 to 2 keeps vehicles and creates trip routes`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val configuration =
            SupportSQLiteOpenHelper.Configuration
                .builder(context)
                .name(MIGRATION_DB)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(1) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            db.execSQL(CREATE_V1_VEHICLES)
                            db.execSQL(
                                "INSERT INTO vehicles (id, name, kind, plate, initialOdometer, tankCapacity, createdAt, isArchived) " +
                                    "VALUES ('00000000-0000-0000-0000-000000000001', 'Gol', 'car', NULL, 1000.0, 55.0, 0, 0)",
                            )
                        }

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) = Unit
                    },
                ).build()

        val openHelper = FrameworkSQLiteOpenHelperFactory().create(configuration)
        val database = openHelper.writableDatabase

        RodaroDatabase.MIGRATION_1_2.migrate(database)

        database.query("SELECT name FROM vehicles").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Gol", cursor.getString(0))
        }
        database.query("SELECT COUNT(*) FROM trip_routes").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
        database.close()
        openHelper.close()
        context.deleteDatabase(MIGRATION_DB)
    }

    @Test
    fun `migrating from 2 to 3 keeps routes and adds toll cost default`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val configuration =
            SupportSQLiteOpenHelper.Configuration
                .builder(context)
                .name(MIGRATION_DB)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(2) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            db.execSQL(CREATE_V2_TRIP_ROUTES)
                            db.execSQL(
                                "INSERT INTO trip_routes (id, vehicleId, origin, destination, distanceKm, isRoundTrip, hasTolls, liters, fuelCost, costPerKilometer, createdAt) " +
                                    "VALUES ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000002', 'A', 'B', 120.0, 0, 1, 10.0, '50', '0.4', 0)",
                            )
                        }

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) = Unit
                    },
                ).build()

        val openHelper = FrameworkSQLiteOpenHelperFactory().create(configuration)
        val database = openHelper.writableDatabase

        RodaroDatabase.MIGRATION_2_3.migrate(database)

        database.query("SELECT fuelCost, tollCost FROM trip_routes").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("50", cursor.getString(0))
            assertEquals("0", cursor.getString(1))
        }
        database.close()
        openHelper.close()
        context.deleteDatabase(MIGRATION_DB)
    }

    @Test
    fun `migrating from 3 to 4 keeps routes and adds consumption snapshot defaults`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val configuration =
            SupportSQLiteOpenHelper.Configuration
                .builder(context)
                .name(MIGRATION_DB)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(3) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            db.execSQL(CREATE_V3_TRIP_ROUTES)
                            db.execSQL(
                                "INSERT INTO trip_routes (id, vehicleId, origin, destination, distanceKm, isRoundTrip, hasTolls, tollCost, liters, fuelCost, costPerKilometer, createdAt) " +
                                    "VALUES ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000002', 'A', 'B', 120.0, 0, 1, '0', 10.0, '50', '0.4', 0)",
                            )
                        }

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) = Unit
                    },
                ).build()

        val openHelper = FrameworkSQLiteOpenHelperFactory().create(configuration)
        val database = openHelper.writableDatabase

        RodaroDatabase.MIGRATION_3_4.migrate(database)

        database.query("SELECT fuelCost, kilometersPerLiter, pricePerLiter FROM trip_routes").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("50", cursor.getString(0))
            assertEquals(0.0, cursor.getDouble(1), 0.001)
            assertEquals("0", cursor.getString(2))
        }
        database.close()
        openHelper.close()
        context.deleteDatabase(MIGRATION_DB)
    }

    @Test
    fun `migrating from 4 to 5 keeps routes with an empty path`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val configuration =
            SupportSQLiteOpenHelper.Configuration
                .builder(context)
                .name(MIGRATION_DB)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(4) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            db.execSQL(CREATE_V3_TRIP_ROUTES)
                            RodaroDatabase.MIGRATION_3_4.migrate(db)
                            db.execSQL(
                                "INSERT INTO trip_routes (id, vehicleId, origin, destination, distanceKm, isRoundTrip, hasTolls, tollCost, kilometersPerLiter, pricePerLiter, liters, fuelCost, costPerKilometer, createdAt) " +
                                    "VALUES ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000002', 'A', 'B', 120.0, 0, 1, '0', 12.0, '6', 10.0, '60', '0.5', 0)",
                            )
                        }

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) = Unit
                    },
                ).build()

        val openHelper = FrameworkSQLiteOpenHelperFactory().create(configuration)
        val database = openHelper.writableDatabase

        RodaroDatabase.MIGRATION_4_5.migrate(database)

        database.query("SELECT origin, path FROM trip_routes").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("A", cursor.getString(0))
            assertEquals("", cursor.getString(1))
        }
        database.close()
        openHelper.close()
        context.deleteDatabase(MIGRATION_DB)
    }

    companion object {
        private const val MIGRATION_DB = "migration-test.db"
        private const val CREATE_V1_VEHICLES =
            "CREATE TABLE IF NOT EXISTS `vehicles` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `kind` TEXT NOT NULL, " +
                "`plate` TEXT, `initialOdometer` REAL NOT NULL, `tankCapacity` REAL, `createdAt` INTEGER NOT NULL, " +
                "`isArchived` INTEGER NOT NULL, PRIMARY KEY(`id`))"
        private const val CREATE_V2_TRIP_ROUTES =
            "CREATE TABLE IF NOT EXISTS `trip_routes` (`id` TEXT NOT NULL, `vehicleId` TEXT NOT NULL, `origin` TEXT NOT NULL, " +
                "`destination` TEXT NOT NULL, `distanceKm` REAL NOT NULL, `isRoundTrip` INTEGER NOT NULL, `hasTolls` INTEGER NOT NULL, " +
                "`liters` REAL NOT NULL, `fuelCost` TEXT NOT NULL, `costPerKilometer` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"
        private const val CREATE_V3_TRIP_ROUTES =
            "CREATE TABLE IF NOT EXISTS `trip_routes` (`id` TEXT NOT NULL, `vehicleId` TEXT NOT NULL, `origin` TEXT NOT NULL, " +
                "`destination` TEXT NOT NULL, `distanceKm` REAL NOT NULL, `isRoundTrip` INTEGER NOT NULL, `hasTolls` INTEGER NOT NULL, " +
                "`tollCost` TEXT NOT NULL, `liters` REAL NOT NULL, `fuelCost` TEXT NOT NULL, `costPerKilometer` TEXT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"
    }
}
