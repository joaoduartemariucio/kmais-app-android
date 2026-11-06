package br.com.joaoduartemariucio.rodaro.presentation.trips.navigation

import androidx.navigation3.runtime.EntryProviderScope
import br.com.joaoduartemariucio.rodaro.presentation.common.navigation.Navigator
import br.com.joaoduartemariucio.rodaro.presentation.common.navigation.Route
import br.com.joaoduartemariucio.rodaro.presentation.trips.RouteDetailScreen
import br.com.joaoduartemariucio.rodaro.presentation.trips.TripCostScreen
import br.com.joaoduartemariucio.rodaro.presentation.trips.TripsListScreen

fun EntryProviderScope<Route>.tripsEntries(navigator: Navigator) {
    entry<TripsRoute.RouteList> {
        TripsListScreen(
            onNewRoute = { navigator.open(TripsRoute.TripCost()) },
            onOpenRoute = { navigator.open(TripsRoute.RouteDetail(it.id)) },
        )
    }
    entry<TripsRoute.RouteDetail> { route ->
        RouteDetailScreen(
            routeId = route.id,
            onEdit = { navigator.open(TripsRoute.TripCost(it)) },
            onBack = navigator::back,
        )
    }
    entry<TripsRoute.TripCost> { route ->
        TripCostScreen(initialRoute = route.route, onBack = navigator::back)
    }
}
