package br.com.joaoduartemariucio.rodaro.data.trips.repositories

import br.com.joaoduartemariucio.rodaro.data.trips.models.TripRouteDao
import br.com.joaoduartemariucio.rodaro.data.trips.models.TripRouteModel
import br.com.joaoduartemariucio.rodaro.domain.common.errors.RepositoryError
import br.com.joaoduartemariucio.rodaro.domain.trips.entities.SavedRoute
import br.com.joaoduartemariucio.rodaro.domain.trips.repositories.RouteRepository
import java.util.UUID

class RoomRouteRepository(
    private val dao: TripRouteDao,
) : RouteRepository {
    override suspend fun routes(vehicleId: UUID?): List<SavedRoute> =
        dao
            .all()
            .filter { vehicleId == null || it.vehicleId == vehicleId }
            .map { it.toDomain() }

    override suspend fun save(route: SavedRoute) {
        try {
            dao.upsert(route.toModel())
        } catch (error: Exception) {
            throw RepositoryError.PersistenceFailed(error)
        }
    }

    override suspend fun delete(id: UUID) {
        dao.delete(id)
    }

    private fun TripRouteModel.toDomain() =
        SavedRoute(
            id = id,
            vehicleId = vehicleId,
            origin = origin,
            destination = destination,
            distanceKm = distanceKm,
            isRoundTrip = isRoundTrip,
            hasTolls = hasTolls,
            liters = liters,
            fuelCost = fuelCost,
            costPerKilometer = costPerKilometer,
            createdAt = createdAt,
        )

    private fun SavedRoute.toModel() =
        TripRouteModel(
            id = id,
            vehicleId = vehicleId,
            origin = origin,
            destination = destination,
            distanceKm = distanceKm,
            isRoundTrip = isRoundTrip,
            hasTolls = hasTolls,
            liters = liters,
            fuelCost = fuelCost,
            costPerKilometer = costPerKilometer,
            createdAt = createdAt,
        )
}
