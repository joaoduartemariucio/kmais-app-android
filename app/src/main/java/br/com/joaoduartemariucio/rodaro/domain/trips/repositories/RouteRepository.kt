package br.com.joaoduartemariucio.rodaro.domain.trips.repositories

import br.com.joaoduartemariucio.rodaro.domain.trips.entities.SavedRoute
import java.util.UUID

interface RouteRepository {
    suspend fun routes(vehicleId: UUID? = null): List<SavedRoute>

    suspend fun save(route: SavedRoute)

    suspend fun delete(id: UUID)
}
