package br.com.joaoduartemariucio.rodaro.presentation.reminders

import android.util.Log
import br.com.joaoduartemariucio.rodaro.R
import br.com.joaoduartemariucio.rodaro.domain.common.services.DateProvider
import br.com.joaoduartemariucio.rodaro.domain.fueling.repositories.FuelEntryRepository
import br.com.joaoduartemariucio.rodaro.domain.maintenance.calculations.MaintenanceForecast
import br.com.joaoduartemariucio.rodaro.domain.maintenance.repositories.ServiceRepository
import br.com.joaoduartemariucio.rodaro.domain.reminders.calculations.PlannedReminder
import br.com.joaoduartemariucio.rodaro.domain.reminders.calculations.ReminderPlan
import br.com.joaoduartemariucio.rodaro.domain.reminders.services.Reminder
import br.com.joaoduartemariucio.rodaro.domain.reminders.services.ReminderScheduler
import br.com.joaoduartemariucio.rodaro.domain.vehicles.entities.VehicleRecords
import br.com.joaoduartemariucio.rodaro.domain.vehicles.repositories.VehicleRepository
import br.com.joaoduartemariucio.rodaro.presentation.common.Localizer
import br.com.joaoduartemariucio.rodaro.presentation.common.formattedFullDate
import br.com.joaoduartemariucio.rodaro.presentation.common.formattedKilometers
import br.com.joaoduartemariucio.rodaro.presentation.common.label
import kotlin.coroutines.cancellation.CancellationException

class ServiceReminderSync(
    private val vehicleRepository: VehicleRepository,
    private val fuelEntryRepository: FuelEntryRepository,
    private val serviceRepository: ServiceRepository,
    private val scheduler: ReminderScheduler,
    private val dateProvider: DateProvider,
    private val localizer: Localizer,
) {
    suspend fun sync() {
        try {
            val planned = ReminderPlan.reminders(records(), dateProvider.now, dateProvider.zone)

            val wanted = planned.map { it.id }.toSet()
            for (id in scheduler.pendingIdentifiers()) {
                if (id !in wanted) scheduler.cancel(id)
            }

            if (planned.isEmpty() || !scheduler.requestAuthorization()) return
            for (reminder in planned) {
                scheduler.schedule(reminder(reminder))
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.e(TAG, "Could not sync service reminders", error)
        }
    }

    private suspend fun records(): List<VehicleRecords> =
        vehicleRepository.all(includingArchived = false).map { vehicle ->
            VehicleRecords(
                vehicle = vehicle,
                fuel = fuelEntryRepository.entries(vehicle.id),
                services = serviceRepository.entries(vehicle.id),
                rules = serviceRepository.rules(vehicle.id),
            )
        }

    fun reminder(planned: PlannedReminder): Reminder {
        val forecast = planned.due.forecast
        val status = planned.status
        val zone = dateProvider.zone

        val lastReading = planned.currentOdometer.formattedKilometers()
        val dueOdometer = forecast.dueOdometer
        val calendarDueDate = status.calendarDueDate
        val dueDate = forecast.dueDate

        val body =
            when {
                status.overdueBy == MaintenanceForecast.Trigger.DISTANCE && dueOdometer != null -> {
                    localizer.string(R.string.reminder_was_due_at, dueOdometer.formattedKilometers(), lastReading)
                }

                status.overdueBy == MaintenanceForecast.Trigger.CALENDAR && calendarDueDate != null -> {
                    localizer.string(R.string.reminder_overdue_since, calendarDueDate.formattedFullDate(zone))
                }

                dueDate != null && forecast.trigger == MaintenanceForecast.Trigger.DISTANCE && dueOdometer != null -> {
                    localizer.string(
                        R.string.reminder_due_at_around,
                        dueOdometer.formattedKilometers(),
                        dueDate.formattedFullDate(zone),
                    )
                }

                dueDate != null -> {
                    localizer.string(R.string.reminder_due_on, dueDate.formattedFullDate(zone))
                }

                else -> {
                    localizer.string(R.string.reminder_due_at, (dueOdometer ?: 0.0).formattedKilometers())
                }
            }

        return Reminder(
            id = planned.id,
            title = "${planned.vehicle.name}: ${localizer.string(planned.due.rule.kind.label)}",
            body = body,
            fireDate = planned.fireDate,
        )
    }

    private companion object {
        const val TAG = "reminders"
    }
}
