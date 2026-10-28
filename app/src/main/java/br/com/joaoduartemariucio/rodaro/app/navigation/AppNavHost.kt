package br.com.joaoduartemariucio.rodaro.app.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import br.com.joaoduartemariucio.rodaro.presentation.common.navigation.Navigator
import br.com.joaoduartemariucio.rodaro.presentation.common.navigation.Route
import br.com.joaoduartemariucio.rodaro.presentation.common.navigation.rememberNavigator
import br.com.joaoduartemariucio.rodaro.presentation.vehicles.navigation.VehiclesRoute
import br.com.joaoduartemariucio.rodaro.presentation.vehicles.navigation.vehiclesEntries

private const val TRANSITION_MILLIS = 300

@Composable
fun AppNavHost(navigator: Navigator = rememberNavigator(start = VehiclesRoute.VehicleList)) {
    NavDisplay(
        backStack = navigator.backStack,
        onBack = navigator::back,
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainer),
        entryDecorators =
            listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
        transitionSpec = { push() },
        popTransitionSpec = { pop() },
        predictivePopTransitionSpec = { pop() },
        entryProvider =
            entryProvider<Route> {
                vehiclesEntries(navigator)
            },
    )
}

private fun push(): ContentTransform =
    slideInHorizontally(tween(TRANSITION_MILLIS, easing = FastOutSlowInEasing)) { it } togetherWith
        slideOutHorizontally(tween(TRANSITION_MILLIS, easing = FastOutSlowInEasing)) { -it / 4 }

private fun pop(): ContentTransform =
    slideInHorizontally(tween(TRANSITION_MILLIS, easing = FastOutSlowInEasing)) { -it / 4 } togetherWith
        slideOutHorizontally(tween(TRANSITION_MILLIS, easing = FastOutSlowInEasing)) { it }
