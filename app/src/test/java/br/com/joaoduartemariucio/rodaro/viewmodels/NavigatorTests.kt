package br.com.joaoduartemariucio.rodaro.viewmodels

import br.com.joaoduartemariucio.rodaro.presentation.common.navigation.Navigator
import br.com.joaoduartemariucio.rodaro.presentation.common.navigation.Route
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NavigatorTests {
    private data object Home : Route

    private data object Other : Route

    private data object Detail : Route

    private fun navigator() = Navigator(listOf(Home, Other))

    @Test
    fun `starts on the first tab`() {
        val navigator = navigator()

        assertEquals(Home, navigator.currentTab)
        assertEquals(listOf<Route>(Home), navigator.backStack)
        assertTrue(navigator.isAtTabRoot)
    }

    @Test
    fun `open pushes on the current tab`() {
        val navigator = navigator()

        navigator.open(Detail)

        assertEquals(listOf<Route>(Home, Detail), navigator.backStack)
        assertEquals(Navigator.Change.PUSH, navigator.lastChange)
        assertFalse(navigator.isAtTabRoot)
    }

    @Test
    fun `open ignores the route already on top`() {
        val navigator = navigator()

        navigator.open(Detail)
        navigator.open(Detail)

        assertEquals(listOf<Route>(Home, Detail), navigator.backStack)
    }

    @Test
    fun `back pops the top route`() {
        val navigator = navigator()
        navigator.open(Detail)

        navigator.back()

        assertEquals(listOf<Route>(Home), navigator.backStack)
        assertEquals(Navigator.Change.POP, navigator.lastChange)
    }

    @Test
    fun `back never empties the start tab`() {
        val navigator = navigator()

        navigator.back()

        assertEquals(listOf<Route>(Home), navigator.backStack)
    }

    @Test
    fun `another tab sits on top of the start tab`() {
        val navigator = navigator()

        navigator.selectTab(Other)

        assertEquals(Other, navigator.currentTab)
        assertEquals(listOf<Route>(Home, Other), navigator.backStack)
        assertEquals(Navigator.Change.TAB, navigator.lastChange)
        assertTrue(navigator.backLeavesTab)
    }

    @Test
    fun `each tab keeps its own stack`() {
        val navigator = navigator()
        navigator.open(Detail)

        navigator.selectTab(Other)
        navigator.selectTab(Home)

        assertEquals(listOf<Route>(Home, Detail), navigator.backStack)
    }

    @Test
    fun `back on the root of another tab returns to the start tab`() {
        val navigator = navigator()
        navigator.selectTab(Other)

        navigator.back()

        assertEquals(Home, navigator.currentTab)
        assertEquals(Navigator.Change.TAB, navigator.lastChange)
    }

    @Test
    fun `selecting the current tab pops it to its root`() {
        val navigator = navigator()
        navigator.selectTab(Other)
        navigator.open(Detail)

        navigator.selectTab(Other)

        assertEquals(listOf<Route>(Home, Other), navigator.backStack)
        assertEquals(Navigator.Change.POP, navigator.lastChange)
        assertTrue(navigator.backLeavesTab)
    }
}
