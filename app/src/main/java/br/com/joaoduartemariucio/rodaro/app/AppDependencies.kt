package br.com.joaoduartemariucio.rodaro.app

import android.content.Context
import android.util.Log
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.app.NotificationManagerCompat
import androidx.work.WorkManager
import br.com.joaoduartemariucio.rodaro.data.PreviewData
import br.com.joaoduartemariucio.rodaro.data.common.services.FirebaseAccountService
import br.com.joaoduartemariucio.rodaro.data.common.services.FirebaseAnalyticsTracker
import br.com.joaoduartemariucio.rodaro.data.common.services.SystemDateProvider
import br.com.joaoduartemariucio.rodaro.data.database.RodaroDatabase
import br.com.joaoduartemariucio.rodaro.data.fueling.repositories.RoomFuelEntryRepository
import br.com.joaoduartemariucio.rodaro.data.maintenance.repositories.RoomServiceRepository
import br.com.joaoduartemariucio.rodaro.data.reminders.services.NotificationPermission
import br.com.joaoduartemariucio.rodaro.data.reminders.services.WorkManagerReminderScheduler
import br.com.joaoduartemariucio.rodaro.data.vehicles.repositories.RoomVehicleRepository
import br.com.joaoduartemariucio.rodaro.domain.common.services.AccountService
import br.com.joaoduartemariucio.rodaro.domain.common.services.Analytics
import br.com.joaoduartemariucio.rodaro.domain.common.services.DateProvider
import br.com.joaoduartemariucio.rodaro.domain.fueling.repositories.FuelEntryRepository
import br.com.joaoduartemariucio.rodaro.domain.maintenance.repositories.ServiceRepository
import br.com.joaoduartemariucio.rodaro.domain.reminders.services.ReminderScheduler
import br.com.joaoduartemariucio.rodaro.domain.vehicles.repositories.VehicleRepository
import br.com.joaoduartemariucio.rodaro.presentation.common.ResourcesLocalizer
import br.com.joaoduartemariucio.rodaro.presentation.reminders.ServiceReminderSync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class AppDependencies(
    context: Context,
    val database: RodaroDatabase,
    val isUsingFallbackStore: Boolean = false,
    val analytics: Analytics = NoAnalytics,
) {
    val vehicleRepository: VehicleRepository = RoomVehicleRepository(database.vehicles())
    val fuelEntryRepository: FuelEntryRepository = RoomFuelEntryRepository(database.fuelEntries(), database.vehicles())
    val serviceRepository: ServiceRepository = RoomServiceRepository(database.services(), database.vehicles())
    val dateProvider: DateProvider = SystemDateProvider()
    val accountService: AccountService = FirebaseAccountService(context)
    val clearLocalData: suspend () -> Unit = {
        withContext(Dispatchers.IO) {
            WorkManager.getInstance(context).cancelAllWork()
            NotificationManagerCompat.from(context).cancelAll()
            database.clearAllTables()
        }
    }
    val notificationPermission = NotificationPermission(context)
    val reminderScheduler: ReminderScheduler = WorkManagerReminderScheduler(context, notificationPermission)
    val reminderSync =
        ServiceReminderSync(
            vehicleRepository = vehicleRepository,
            fuelEntryRepository = fuelEntryRepository,
            serviceRepository = serviceRepository,
            scheduler = reminderScheduler,
            dateProvider = dateProvider,
            localizer = ResourcesLocalizer(context.resources),
        )

    companion object {
        fun live(context: Context): AppDependencies =
            try {
                val database = RodaroDatabase.create(context)
                database.openHelper.writableDatabase
                AppDependencies(context, database, analytics = FirebaseAnalyticsTracker(context))
            } catch (error: Exception) {
                Log.e("startup", "On-disk store unavailable, falling back to memory", error)
                inMemory(context, isUsingFallbackStore = true, analytics = FirebaseAnalyticsTracker(context))
            }

        fun inMemory(
            context: Context,
            isUsingFallbackStore: Boolean = false,
            analytics: Analytics = NoAnalytics,
        ): AppDependencies = AppDependencies(context, RodaroDatabase.create(context, inMemory = true), isUsingFallbackStore, analytics)

        fun preview(context: Context): AppDependencies = inMemory(context).also { runBlocking { PreviewData.seed(it.database) } }
    }
}

val LocalDependencies = staticCompositionLocalOf<AppDependencies> { error("AppDependencies not provided") }
