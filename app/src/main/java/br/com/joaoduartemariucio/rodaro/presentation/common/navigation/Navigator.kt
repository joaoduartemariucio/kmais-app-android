package br.com.joaoduartemariucio.rodaro.presentation.common.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList

class Navigator(
    val tabs: List<Route>,
) {
    enum class Change {
        PUSH,
        POP,
        TAB,
    }

    init {
        require(tabs.isNotEmpty())
    }

    private val startTab: Route = tabs.first()
    private val stacks: Map<Route, SnapshotStateList<Route>> = tabs.associateWith { mutableStateListOf(it) }

    var currentTab: Route by mutableStateOf(startTab)
        private set

    var lastChange: Change = Change.PUSH
        private set

    val backStack: List<Route>
        get() = if (currentTab == startTab) stack(startTab).toList() else stack(startTab) + stack(currentTab)

    val isAtTabRoot: Boolean
        get() = stack(currentTab).size == 1

    val backLeavesTab: Boolean
        get() = isAtTabRoot && currentTab != startTab

    fun selectTab(tab: Route) {
        require(tab in stacks)
        if (tab == currentTab) {
            popToRoot()
        } else {
            currentTab = tab
            lastChange = Change.TAB
        }
    }

    fun open(route: Route) {
        val stack = stack(currentTab)
        if (stack.last() == route) return
        stack.add(route)
        lastChange = Change.PUSH
    }

    fun back() {
        val stack = stack(currentTab)
        when {
            stack.size > 1 -> {
                stack.removeAt(stack.lastIndex)
                lastChange = Change.POP
            }

            currentTab != startTab -> {
                currentTab = startTab
                lastChange = Change.TAB
            }
        }
    }

    private fun popToRoot() {
        val stack = stack(currentTab)
        if (stack.size == 1) return
        stack.removeRange(1, stack.size)
        lastChange = Change.POP
    }

    private fun stack(tab: Route): SnapshotStateList<Route> = stacks.getValue(tab)
}

@Composable
fun rememberNavigator(tabs: List<Route>): Navigator = remember { Navigator(tabs) }
