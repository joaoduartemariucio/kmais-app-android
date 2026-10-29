package br.com.joaoduartemariucio.rodaro.app.navigation

import androidx.annotation.StringRes
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import br.com.joaoduartemariucio.rodaro.R
import br.com.joaoduartemariucio.rodaro.app.LocalDependencies
import br.com.joaoduartemariucio.rodaro.presentation.common.navigation.Navigator
import br.com.joaoduartemariucio.rodaro.presentation.common.navigation.Route
import br.com.joaoduartemariucio.rodaro.presentation.common.navigation.rememberNavigator
import br.com.joaoduartemariucio.rodaro.presentation.profile.navigation.ProfileRoute
import br.com.joaoduartemariucio.rodaro.presentation.profile.navigation.profileEntries
import br.com.joaoduartemariucio.rodaro.presentation.trips.navigation.TripsRoute
import br.com.joaoduartemariucio.rodaro.presentation.trips.navigation.tripsEntries
import br.com.joaoduartemariucio.rodaro.presentation.vehicles.navigation.VehiclesRoute
import br.com.joaoduartemariucio.rodaro.presentation.vehicles.navigation.vehiclesEntries

private const val TRANSITION_MILLIS = 300

private data class Tab(
    val route: Route,
    @param:StringRes val label: Int,
    val icon: ImageVector,
)

private val tabs =
    listOf(
        Tab(VehiclesRoute.VehicleList, R.string.vehicles, Icons.Filled.DirectionsCar),
        Tab(VehiclesRoute.ArchivedVehicles, R.string.archived, Icons.Filled.Archive),
        Tab(TripsRoute.TripCost, R.string.route, Icons.Filled.Route),
        Tab(ProfileRoute.Profile, R.string.profile, Icons.Filled.Person),
    )

@Composable
fun AppNavHost(navigator: Navigator = rememberNavigator(tabs.map { it.route })) {
    val analytics = LocalDependencies.current.analytics
    val screen = navigator.backStack.last()
    LaunchedEffect(screen) { analytics.screen(screen::class.simpleName ?: screen.toString()) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentWindowInsets = WindowInsets(0),
        bottomBar = { TabBar(navigator) },
    ) { padding ->
        NavDisplay(
            backStack = navigator.backStack,
            onBack = navigator::back,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .background(MaterialTheme.colorScheme.surfaceContainer),
            entryDecorators =
                listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
            transitionSpec = { if (navigator.lastChange == Navigator.Change.TAB) instant() else push() },
            popTransitionSpec = { if (navigator.lastChange == Navigator.Change.TAB) instant() else pop() },
            predictivePopTransitionSpec = { if (navigator.backLeavesTab) instant() else pop() },
            entryProvider =
                entryProvider<Route> {
                    vehiclesEntries(navigator)
                    tripsEntries()
                    profileEntries(navigator)
                },
        )
    }
}

@Composable
private fun TabBar(navigator: Navigator) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = navigator.currentTab == tab.route,
                onClick = { navigator.selectTab(tab.route) },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(stringResource(tab.label)) },
            )
        }
    }
}

private fun instant(): ContentTransform = EnterTransition.None togetherWith ExitTransition.None

private fun push(): ContentTransform =
    slideInHorizontally(tween(TRANSITION_MILLIS, easing = FastOutSlowInEasing)) { it } togetherWith
        slideOutHorizontally(tween(TRANSITION_MILLIS, easing = FastOutSlowInEasing)) { -it / 4 }

private fun pop(): ContentTransform =
    slideInHorizontally(tween(TRANSITION_MILLIS, easing = FastOutSlowInEasing)) { -it / 4 } togetherWith
        slideOutHorizontally(tween(TRANSITION_MILLIS, easing = FastOutSlowInEasing)) { it }
