package br.com.joaoduartemariucio.rodaro.fakes

import br.com.joaoduartemariucio.rodaro.domain.common.entities.sortedChronologically
import br.com.joaoduartemariucio.rodaro.domain.common.errors.RepositoryError
import br.com.joaoduartemariucio.rodaro.domain.common.services.Analytics
import br.com.joaoduartemariucio.rodaro.domain.common.services.DateProvider
import br.com.joaoduartemariucio.rodaro.domain.fueling.entities.FuelEntry
import br.com.joaoduartemariucio.rodaro.domain.fueling.repositories.FuelEntryRepository
import br.com.joaoduartemariucio.rodaro.domain.maintenance.entities.ServiceEntry
import br.com.joaoduartemariucio.rodaro.domain.maintenance.entities.ServiceRule
import br.com.joaoduartemariucio.rodaro.domain.maintenance.repositories.ServiceRepository
import br.com.joaoduartemariucio.rodaro.domain.preferences.entities.Preferences
import br.com.joaoduartemariucio.rodaro.domain.preferences.repositories.PreferencesRepository
import br.com.joaoduartemariucio.rodaro.domain.reminders.services.Reminder
import br.com.joaoduartemariucio.rodaro.domain.reminders.services.ReminderScheduler
import br.com.joaoduartemariucio.rodaro.domain.trips.entities.RouteEstimate
import br.com.joaoduartemariucio.rodaro.domain.trips.entities.SavedRoute
import br.com.joaoduartemariucio.rodaro.domain.trips.repositories.RouteRepository
import br.com.joaoduartemariucio.rodaro.domain.trips.services.RouteEstimateProvider
import br.com.joaoduartemariucio.rodaro.domain.vehicles.entities.Vehicle
import br.com.joaoduartemariucio.rodaro.domain.vehicles.repositories.VehicleRepository
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID

class InMemoryVehicleRepository(
    vehicles: List<Vehicle> = emptyList(),
    var failure: Exception? = null,
) : VehicleRepository {
    private val storage = vehicles.associateBy { it.id }.toMutableMap()

    override suspend fun all(includingArchived: Boolean): List<Vehicle> {
        failure?.let { throw it }
        return storage.values.filter { includingArchived || !it.isArchived }.sortedByDescending { it.createdAt }
    }

    override suspend fun vehicle(id: UUID): Vehicle? {
        failure?.let { throw it }
        return storage[id]
    }

    override suspend fun save(vehicle: Vehicle) {
        failure?.let { throw it }
        storage[vehicle.id] = vehicle
    }

    override suspend fun setArchived(
        isArchived: Boolean,
        vehicleId: UUID,
    ) {
        failure?.let { throw it }
        val vehicle = storage[vehicleId] ?: throw RepositoryError.VehicleNotFound(vehicleId)
        storage[vehicleId] = vehicle.copy(isArchived = isArchived)
    }
}

class InMemoryFuelEntryRepository(
    entries: List<FuelEntry> = emptyList(),
    var failure: Exception? = null,
) : FuelEntryRepository {
    private val storage = entries.associateBy { it.id }.toMutableMap()

    override suspend fun entries(vehicleId: UUID): List<FuelEntry> {
        failure?.let { throw it }
        return storage.values.filter { it.vehicleId == vehicleId }.sortedChronologically()
    }

    override suspend fun save(entry: FuelEntry) {
        failure?.let { throw it }
        storage[entry.id] = entry
    }

    override suspend fun delete(id: UUID) {
        failure?.let { throw it }
        storage.remove(id) ?: throw RepositoryError.EntryNotFound(id)
    }
}

class InMemoryServiceRepository(
    entries: List<ServiceEntry> = emptyList(),
    rules: List<ServiceRule> = emptyList(),
    var failure: Exception? = null,
) : ServiceRepository {
    private val entryStorage = entries.associateBy { it.id }.toMutableMap()
    private val ruleStorage = rules.associateBy { it.id }.toMutableMap()

    override suspend fun entries(vehicleId: UUID): List<ServiceEntry> {
        failure?.let { throw it }
        return entryStorage.values.filter { it.vehicleId == vehicleId }.sortedChronologically()
    }

    override suspend fun rules(vehicleId: UUID): List<ServiceRule> {
        failure?.let { throw it }
        return ruleStorage.values.filter { it.vehicleId == vehicleId }
    }

    override suspend fun save(entry: ServiceEntry) {
        failure?.let { throw it }
        entryStorage[entry.id] = entry
    }

    override suspend fun save(
        rules: List<ServiceRule>,
        vehicleId: UUID,
    ) {
        failure?.let { throw it }
        val kept = rules.map { it.id }.toSet()
        ruleStorage.values.removeAll { it.vehicleId == vehicleId && it.id !in kept }
        rules.forEach { ruleStorage[it.id] = it }
    }

    override suspend fun deleteEntry(id: UUID) {
        failure?.let { throw it }
        entryStorage.remove(id) ?: throw RepositoryError.EntryNotFound(id)
    }

    override suspend fun deleteRule(id: UUID) {
        failure?.let { throw it }
        ruleStorage.remove(id) ?: throw RepositoryError.EntryNotFound(id)
    }
}

class FixedDateProvider(
    override val now: Instant,
    override val zone: ZoneId = ZoneOffset.UTC,
) : DateProvider

class InMemoryPreferencesRepository(
    var stored: Preferences = Preferences(),
    var failure: Exception? = null,
) : PreferencesRepository {
    var saves = 0
        private set

    override suspend fun get(): Preferences {
        failure?.let { throw it }
        return stored
    }

    override suspend fun save(preferences: Preferences) {
        failure?.let { throw it }
        stored = preferences
        saves += 1
    }
}

class FakeReminderScheduler(
    pending: List<Reminder> = emptyList(),
    private val isAuthorized: Boolean = true,
) : ReminderScheduler {
    val scheduled = pending.associateBy { it.id }.toMutableMap()
    var authorizationRequests = 0
        private set

    override suspend fun requestAuthorization(): Boolean {
        authorizationRequests += 1
        return isAuthorized
    }

    override suspend fun schedule(reminder: Reminder) {
        scheduled[reminder.id] = reminder
    }

    override suspend fun cancel(id: UUID) {
        scheduled.remove(id)
    }

    override suspend fun pendingIdentifiers(): List<UUID> = scheduled.keys.toList()
}

class FakeAnalytics : Analytics {
    data class Recorded(
        val name: String,
        val parameters: Map<String, String>,
    )

    val events = mutableListOf<Recorded>()
    val screens = mutableListOf<String>()

    override fun screen(name: String) {
        screens += name
    }

    override fun event(
        name: String,
        parameters: Map<String, String>,
    ) {
        events += Recorded(name, parameters)
    }

    fun names(): List<String> = events.map { it.name }

    fun parameters(name: String): Map<String, String>? = events.lastOrNull { it.name == name }?.parameters
}

class FakeRouteEstimateProvider(
    var estimate: RouteEstimate = RouteEstimate(distanceKm = 120.0, durationMinutes = 90, hasTolls = true),
    var failure: Exception? = null,
) : RouteEstimateProvider {
    val lookups = mutableListOf<Pair<String, String>>()

    override suspend fun estimate(
        origin: String,
        destination: String,
    ): RouteEstimate {
        failure?.let { throw it }
        lookups += origin to destination
        return estimate
    }
}

class InMemoryRouteRepository(
    routes: List<SavedRoute> = emptyList(),
) : RouteRepository {
    private val storage = routes.associateBy { it.id }.toMutableMap()

    override suspend fun routes(vehicleId: UUID?): List<SavedRoute> = storage.values.filter { vehicleId == null || it.vehicleId == vehicleId }.sortedByDescending { it.createdAt }

    override suspend fun save(route: SavedRoute) {
        storage[route.id] = route
    }

    override suspend fun delete(id: UUID) {
        storage.remove(id)
    }
}
