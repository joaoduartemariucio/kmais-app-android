package br.com.joaoduartemariucio.rodaro.data

import br.com.joaoduartemariucio.rodaro.data.network.Polyline
import br.com.joaoduartemariucio.rodaro.domain.trips.entities.RoutePoint
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PolylineTests {
    @Test
    fun `decodes the reference polyline with five digits`() {
        val points = Polyline.decode("_p~iF~ps|U_ulLnnqC_mqNvxq`@", Polyline.PRECISION_5)

        assertEquals(listOf(RoutePoint(38.5, -120.2), RoutePoint(40.7, -120.95), RoutePoint(43.252, -126.453)), points)
    }

    @Test
    fun `encoding and decoding with six digits round-trips`() {
        val path = listOf(RoutePoint(-23.550520, -46.633308), RoutePoint(-22.906847, -43.172897), RoutePoint(-22.9, -43.17))

        val encoded = Polyline.encode(path, Polyline.PRECISION_6)

        assertEquals(path, Polyline.decode(encoded, Polyline.PRECISION_6))
    }

    @Test
    fun `an empty or truncated polyline decodes to the complete points only`() {
        assertTrue(Polyline.decode("", Polyline.PRECISION_6).isEmpty())
        assertEquals("", Polyline.encode(emptyList(), Polyline.PRECISION_6))
        assertEquals(listOf(RoutePoint(38.5, -120.2)), Polyline.decode("_p~iF~ps|U_ulL", Polyline.PRECISION_5))
    }
}
