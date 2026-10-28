package br.com.joaoduartemariucio.rodaro.presentation.common.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList

class Navigator(
    start: Route,
) {
    val backStack: SnapshotStateList<Route> = mutableStateListOf(start)

    fun open(route: Route) {
        if (backStack.lastOrNull() != route) backStack.add(route)
    }

    fun back() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }
}

@Composable
fun rememberNavigator(start: Route): Navigator = remember { Navigator(start) }
