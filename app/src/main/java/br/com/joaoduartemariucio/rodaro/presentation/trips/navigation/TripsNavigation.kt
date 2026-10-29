package br.com.joaoduartemariucio.rodaro.presentation.trips.navigation

import androidx.navigation3.runtime.EntryProviderScope
import br.com.joaoduartemariucio.rodaro.presentation.common.navigation.Route
import br.com.joaoduartemariucio.rodaro.presentation.trips.TripCostScreen

fun EntryProviderScope<Route>.tripsEntries() {
    entry<TripsRoute.TripCost> { TripCostScreen() }
}
