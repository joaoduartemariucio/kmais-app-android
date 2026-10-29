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

    companion object {
        private const val MIGRATION_DB = "migration-test.db"
        private const val CREATE_V1_VEHICLES =
            "CREATE TABLE IF NOT EXISTS `vehicles` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `kind` TEXT NOT NULL, " +
                "`plate` TEXT, `initialOdometer` REAL NOT NULL, `tankCapacity` REAL, `createdAt` INTEGER NOT NULL, " +
                "`isArchived` INTEGER NOT NULL, PRIMARY KEY(`id`))"
    }
}
