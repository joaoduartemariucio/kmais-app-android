package br.com.joaoduartemariucio.kmais_app_android.app

import android.content.Context
import android.util.Log
import androidx.compose.runtime.staticCompositionLocalOf
import br.com.joaoduartemariucio.kmais_app_android.data.PreviewData
import br.com.joaoduartemariucio.kmais_app_android.data.models.KmaisDatabase
import br.com.joaoduartemariucio.kmais_app_android.data.repositories.RoomFuelEntryRepository
import br.com.joaoduartemariucio.kmais_app_android.data.repositories.RoomServiceRepository
import br.com.joaoduartemariucio.kmais_app_android.data.repositories.RoomVehicleRepository
import br.com.joaoduartemariucio.kmais_app_android.data.services.NotificationPermission
import br.com.joaoduartemariucio.kmais_app_android.data.services.SystemDateProvider
import br.com.joaoduartemariucio.kmais_app_android.data.services.WorkManagerReminderScheduler
import br.com.joaoduartemariucio.kmais_app_android.domain.repositories.FuelEntryRepository
import br.com.joaoduartemariucio.kmais_app_android.domain.repositories.ServiceRepository
import br.com.joaoduartemariucio.kmais_app_android.domain.repositories.VehicleRepository
import br.com.joaoduartemariucio.kmais_app_android.domain.services.DateProvider
import br.com.joaoduartemariucio.kmais_app_android.domain.services.ReminderScheduler
import br.com.joaoduartemariucio.kmais_app_android.presentation.common.ResourcesLocalizer
import br.com.joaoduartemariucio.kmais_app_android.presentation.reminders.ServiceReminderSync
import kotlinx.coroutines.runBlocking

class AppDependencies(
    context: Context,
    val database: KmaisDatabase,
    val isUsingFallbackStore: Boolean = false,
) {
    val vehicleRepository: VehicleRepository = RoomVehicleRepository(database.vehicles())
    val fuelEntryRepository: FuelEntryRepository = RoomFuelEntryRepository(database.fuelEntries(), database.vehicles())
    val serviceRepository: ServiceRepository = RoomServiceRepository(database.services(), database.vehicles())
    val dateProvider: DateProvider = SystemDateProvider()
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
                val database = KmaisDatabase.create(context)
                database.openHelper.writableDatabase
                AppDependencies(context, database)
            } catch (error: Exception) {
                Log.e("startup", "On-disk store unavailable, falling back to memory", error)
                inMemory(context, isUsingFallbackStore = true)
            }

        fun inMemory(
            context: Context,
            isUsingFallbackStore: Boolean = false,
        ): AppDependencies = AppDependencies(context, KmaisDatabase.create(context, inMemory = true), isUsingFallbackStore)

        fun preview(context: Context): AppDependencies = inMemory(context).also { runBlocking { PreviewData.seed(it.database) } }
    }
}

val LocalDependencies = staticCompositionLocalOf<AppDependencies> { error("AppDependencies not provided") }
