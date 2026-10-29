package br.com.joaoduartemariucio.rodaro.data

import br.com.joaoduartemariucio.rodaro.data.network.OrsNetwork
import br.com.joaoduartemariucio.rodaro.data.trips.repositories.OpenRouteServiceProvider
import br.com.joaoduartemariucio.rodaro.domain.common.errors.RouteLookupError
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class OpenRouteServiceProviderTests {
    private lateinit var server: MockWebServer

    private val provider by lazy { OpenRouteServiceProvider(OrsNetwork.api("test-key", server.url("/").toString())) }

    private val geocode =
        MockResponse().setBody(
            """
            {"features":[{"geometry":{"coordinates":[-46.633308,-23.55052]}}]}
            """.trimIndent(),
        )

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `a route with a toll section reports distance, duration and tolls`() =
        runTest {
            server.enqueue(geocode)
            server.enqueue(geocode)
            server.enqueue(
                MockResponse().setBody(
                    """
                    {"routes":[{"summary":{"distance":123456.0,"duration":5400.0},"extras":{"tollways":{"summary":[{"value":1,"distance":2000.0,"amount":15.0}]}}}]}
                    """.trimIndent(),
                ),
            )

            val estimate = provider.estimate("São Paulo, SP", "Rio de Janeiro, RJ")

            assertEquals(123.456, estimate.distanceKm, 0.001)
            assertEquals(90, estimate.durationMinutes)
            assertTrue(estimate.hasTolls)
            assertEquals("test-key", server.takeRequest().getHeader("Authorization"))
        }

    @Test
    fun `a route without a tollways extra reports no tolls`() =
        runTest {
            server.enqueue(geocode)
            server.enqueue(geocode)
            server.enqueue(
                MockResponse().setBody(
                    """
                    {"routes":[{"summary":{"distance":1000.0,"duration":300.0}}]}
                    """.trimIndent(),
                ),
            )

            val estimate = provider.estimate("A", "B")

            assertEquals(1.0, estimate.distanceKm, 0.001)
            assertEquals(5, estimate.durationMinutes)
            assertEquals(false, estimate.hasTolls)
        }

    @Test
    fun `an unknown place fails with place not found`() =
        runTest {
            server.enqueue(MockResponse().setBody("""{"features":[]}"""))

            val error =
                runCatching { provider.estimate("Nowhere", "Rio de Janeiro, RJ") }.exceptionOrNull()

            assertIs<RouteLookupError.PlaceNotFound>(error)
            assertEquals("Nowhere", error.text)
        }

    @Test
    fun `a server failure fails with request failed`() =
        runTest {
            server.enqueue(geocode)
            server.enqueue(geocode)
            server.enqueue(MockResponse().setResponseCode(500))

            val error =
                runCatching { provider.estimate("A", "B") }.exceptionOrNull()

            assertIs<RouteLookupError.RequestFailed>(error)
        }
}
