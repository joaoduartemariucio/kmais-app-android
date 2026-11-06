package br.com.joaoduartemariucio.rodaro.presentation.trips.navigation

import br.com.joaoduartemariucio.rodaro.domain.trips.entities.SavedRoute
import br.com.joaoduartemariucio.rodaro.presentation.common.navigation.Route
import java.util.UUID

sealed interface TripsRoute : Route {
    data object RouteList : TripsRoute

    data class RouteDetail(
        val id: UUID,
    ) : TripsRoute

    data class TripCost(
        val route: SavedRoute? = null,
    ) : TripsRoute
}
