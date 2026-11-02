package br.com.joaoduartemariucio.rodaro.viewmodels

import br.com.joaoduartemariucio.rodaro.domain.trips.entities.RoutePoint
import br.com.joaoduartemariucio.rodaro.presentation.trips.ExternalMaps
import org.junit.Test
import kotlin.test.assertEquals

class ExternalMapsTests {
    @Test
    fun `google maps opens driving directions between the typed places`() {
        assertEquals(
            "https://www.google.com/maps/dir/?api=1&origin=S%C3%A3o%20Paulo%2C%20SP&destination=Campinas&travelmode=driving",
            ExternalMaps.googleMaps(" São Paulo, SP ", "Campinas"),
        )
    }

    @Test
    fun `waze navigates to the route end, or searches the destination without one`() {
        assertEquals(
            "https://waze.com/ul?ll=-22.90424,-47.109468&navigate=yes",
            ExternalMaps.waze("Campinas", RoutePoint(-22.90424, -47.109468)),
        )
        assertEquals("https://waze.com/ul?q=Campinas%2C%20SP&navigate=yes", ExternalMaps.waze("Campinas, SP", null))
    }
}
