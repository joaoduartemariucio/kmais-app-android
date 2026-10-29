package br.com.joaoduartemariucio.rodaro.presentation.trips.navigation

import br.com.joaoduartemariucio.rodaro.presentation.common.navigation.Route

sealed interface TripsRoute : Route {
    data object TripCost : TripsRoute
}
