package br.com.joaoduartemariucio.rodaro.viewmodels

import br.com.joaoduartemariucio.rodaro.presentation.common.navigation.Navigator
import br.com.joaoduartemariucio.rodaro.presentation.common.navigation.Route
import org.junit.Test
import kotlin.test.assertEquals

class NavigatorTests {
    private data object Start : Route

    private data object Next : Route

    @Test
    fun `open pushes a new route`() {
        val navigator = Navigator(Start)

        navigator.open(Next)

        assertEquals(listOf<Route>(Start, Next), navigator.backStack.toList())
    }

    @Test
    fun `open ignores the route already on top`() {
        val navigator = Navigator(Start)

        navigator.open(Next)
        navigator.open(Next)

        assertEquals(listOf<Route>(Start, Next), navigator.backStack.toList())
    }

    @Test
    fun `back pops the top route`() {
        val navigator = Navigator(Start)
        navigator.open(Next)

        navigator.back()

        assertEquals(listOf<Route>(Start), navigator.backStack.toList())
    }

    @Test
    fun `back never empties the stack`() {
        val navigator = Navigator(Start)

        navigator.back()

        assertEquals(listOf<Route>(Start), navigator.backStack.toList())
    }
}
