package br.com.joaoduartemariucio.rodaro.data

import br.com.joaoduartemariucio.rodaro.data.network.DirectionsRequest
import br.com.joaoduartemariucio.rodaro.data.network.DirectionsResponse
import br.com.joaoduartemariucio.rodaro.data.network.GeocodeResponse
import br.com.joaoduartemariucio.rodaro.data.network.OrsApi
import br.com.joaoduartemariucio.rodaro.data.network.OrsNetwork
import br.com.joaoduartemariucio.rodaro.data.network.Polyline
import br.com.joaoduartemariucio.rodaro.data.network.RodaroNetwork
import br.com.joaoduartemariucio.rodaro.data.trips.repositories.OpenRouteServiceProvider
import br.com.joaoduartemariucio.rodaro.domain.common.errors.RouteLookupError
import br.com.joaoduartemariucio.rodaro.domain.vehicles.entities.VehicleKind
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.math.BigDecimal
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.coroutines.cancellation.CancellationException
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
                    {"routes":[{"summary":{"distance":123456.0,"duration":5400.0},"geometry":"_p~iF~ps|U_ulLnnqC","extras":{"tollways":{"summary":[{"value":1,"distance":2000.0,"amount":15.0}]}}}]}
                    """.trimIndent(),
                ),
            )

            val estimate = provider.estimate("São Paulo, SP", "Rio de Janeiro, RJ")

            assertEquals(123.456, estimate.distanceKm, 0.001)
            assertEquals(90, estimate.durationMinutes)
            assertTrue(estimate.hasTolls)
            assertEquals(2, estimate.path.size)
            val firstRequest = server.takeRequest()
            assertEquals("test-key", firstRequest.getHeader("Authorization"))
            assertTrue(firstRequest.path.orEmpty().contains("api_key=test-key"))
        }

    @Test
    fun `the rodaro api supplies distance and the toll cost for both directions`() =
        runTest {
            val withTolls =
                OpenRouteServiceProvider(
                    api = OrsNetwork.api("test-key", server.url("/").toString()),
                    routes = RodaroNetwork.api(server.url("/").toString()),
                )
            server.enqueue(geocode)
            server.enqueue(geocode)
            server.enqueue(
                MockResponse().setBody(
                    """
                    {"outbound":[{"distanceKm":433.62,"durationMinutes":335,"geometry":"_izlhA~rlgdF_{geC~ywl@","tolls":{"total":66.4,"charges":[{"name":"Arujá","value":4.6}]}}],
                     "inbound":[{"distanceKm":433.1,"durationMinutes":330,"tolls":{"total":61.8,"charges":[{"name":"Arujá","value":4.6}]}}]}
                    """.trimIndent(),
                ),
            )

            val estimate = withTolls.estimate("São Paulo, SP", "Rio de Janeiro, RJ", VehicleKind.MOTORCYCLE)

            assertEquals(433.62, estimate.distanceKm, 0.001)
            assertEquals(335, estimate.durationMinutes)
            assertTrue(estimate.hasTolls)
            assertEquals(BigDecimal("66.40"), estimate.tollCost)
            assertEquals(BigDecimal("128.20"), estimate.roundTripTollCost)
            assertEquals(Polyline.encode(estimate.path, Polyline.PRECISION_6), "_izlhA~rlgdF_{geC~ywl@")
            assertEquals(2, estimate.path.size)
            server.takeRequest()
            server.takeRequest()
            val routeRequest = server.takeRequest()
            assertEquals("/v1/routes", routeRequest.path)
            val body = routeRequest.body.readUtf8()
            assertTrue(body.contains("\"vehicle\":\"motorcycle\""))
            assertTrue(body.contains("\"roundTrip\":true"))
            assertTrue(body.contains("\"lat\":-23.55052"))
        }

    @Test
    fun `a rodaro route without the return doubles the one way toll and a missing route fails`() =
        runTest {
            val withTolls =
                OpenRouteServiceProvider(
                    api = OrsNetwork.api("test-key", server.url("/").toString()),
                    routes = RodaroNetwork.api(server.url("/").toString()),
                )
            server.enqueue(geocode)
            server.enqueue(geocode)
            server.enqueue(
                MockResponse().setBody(
                    """
                    {"outbound":[{"distanceKm":80.0,"durationMinutes":60,"tolls":{"total":0,"charges":[]}}],"inbound":null}
                    """.trimIndent(),
                ),
            )

            val estimate = withTolls.estimate("A", "B")

            assertEquals(false, estimate.hasTolls)
            assertEquals(BigDecimal("0.00"), estimate.roundTripTollCost)

            server.enqueue(geocode)
            server.enqueue(geocode)
            server.enqueue(MockResponse().setResponseCode(404).setBody("{}"))
            assertIs<RouteLookupError.RequestFailed>(runCatching { withTolls.estimate("A", "B") }.exceptionOrNull())
        }

    @Test
    fun `autocomplete returns display labels`() =
        runTest {
            server.enqueue(
                MockResponse().setBody(
                    """
                    {"features":[{"geometry":{"coordinates":[-46.6,-23.5]},"properties":{"label":"São Paulo, SP, Brasil"}},{"geometry":{"coordinates":[-43.2,-22.9]},"properties":{"name":"Rio de Janeiro"}}]}
                    """.trimIndent(),
                ),
            )

            val suggestions = provider.suggestions("sao paulo")

            assertEquals(listOf("São Paulo, SP, Brasil", "Rio de Janeiro"), suggestions.map { it.label })
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
    fun `a route with tollway bit in waycategory reports tolls`() =
        runTest {
            server.enqueue(geocode)
            server.enqueue(geocode)
            server.enqueue(
                MockResponse().setBody(
                    """
                    {"routes":[{"summary":{"distance":1000.0,"duration":300.0},"extras":{"waycategory":{"summary":[{"value":3}]}}}]}
                    """.trimIndent(),
                ),
            )

            val estimate = provider.estimate("A", "B")

            assertTrue(estimate.hasTolls)
        }

    @Test
    fun `a route with tollway flag in waycategory values reports tolls`() =
        runTest {
            server.enqueue(geocode)
            server.enqueue(geocode)
            server.enqueue(
                MockResponse().setBody(
                    """
                    {"routes":[{"summary":{"distance":1000.0,"duration":300.0},"extras":{"waycategory":{"values":[[0,10,2]],"summary":[]}}}]}
                    """.trimIndent(),
                ),
            )

            val estimate = provider.estimate("A", "B")

            assertTrue(estimate.hasTolls)
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
    fun `http 401 maps to invalid api key`() =
        runTest {
            server.enqueue(geocode)
            server.enqueue(geocode)
            server.enqueue(MockResponse().setResponseCode(401))

            val error = runCatching { provider.estimate("A", "B") }.exceptionOrNull()

            assertIs<RouteLookupError.InvalidApiKey>(error)
        }

    @Test
    fun `http 429 maps to quota exceeded`() =
        runTest {
            server.enqueue(geocode)
            server.enqueue(geocode)
            server.enqueue(MockResponse().setResponseCode(429))

            val error = runCatching { provider.estimate("A", "B") }.exceptionOrNull()

            assertIs<RouteLookupError.QuotaExceeded>(error)
        }

    @Test
    fun `network errors map to network unavailable`() =
        runTest {
            val directionsNetwork =
                OpenRouteServiceProvider(
                    FakeOrsApi(
                        geocodeCall = { GeocodeResponse(features = geocodeFeatures()) },
                        directionsCall = { throw UnknownHostException("offline") },
                    ),
                )
            val geocodeNetwork =
                OpenRouteServiceProvider(
                    FakeOrsApi(
                        geocodeCall = { throw SocketTimeoutException("timeout") },
                        directionsCall = { DirectionsResponse() },
                    ),
                )
            val ioNetwork =
                OpenRouteServiceProvider(
                    FakeOrsApi(
                        geocodeCall = { GeocodeResponse(features = geocodeFeatures()) },
                        directionsCall = { throw IOException("io") },
                    ),
                )

            assertIs<RouteLookupError.NetworkUnavailable>(runCatching { directionsNetwork.estimate("A", "B") }.exceptionOrNull())
            assertIs<RouteLookupError.NetworkUnavailable>(runCatching { geocodeNetwork.estimate("A", "B") }.exceptionOrNull())
            assertIs<RouteLookupError.NetworkUnavailable>(runCatching { ioNetwork.estimate("A", "B") }.exceptionOrNull())
        }

    @Test
    fun `suggestions returns empty list on generic failure and rethrows cancellation`() =
        runTest {
            val generic =
                OpenRouteServiceProvider(
                    FakeOrsApi(
                        geocodeCall = { GeocodeResponse() },
                        directionsCall = { DirectionsResponse() },
                        autocompleteCall = { throw IllegalStateException("boom") },
                    ),
                )
            val cancellation =
                OpenRouteServiceProvider(
                    FakeOrsApi(
                        geocodeCall = { GeocodeResponse() },
                        directionsCall = { DirectionsResponse() },
                        autocompleteCall = { throw CancellationException("cancel") },
                    ),
                )

            assertTrue(generic.suggestions("sao").isEmpty())
            assertIs<CancellationException>(runCatching { cancellation.suggestions("sao") }.exceptionOrNull())
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

    private fun geocodeFeatures() =
        listOf(
            br.com.joaoduartemariucio.rodaro.data.network.GeocodeFeature(
                geometry =
                    br.com.joaoduartemariucio.rodaro.data.network
                        .GeocodeGeometry(listOf(-46.6, -23.5)),
            ),
        )

    private class FakeOrsApi(
        private val geocodeCall: suspend () -> GeocodeResponse,
        private val directionsCall: suspend () -> DirectionsResponse,
        private val autocompleteCall: suspend () -> GeocodeResponse = geocodeCall,
    ) : OrsApi {
        override suspend fun autocomplete(
            text: String,
            size: Int,
        ): GeocodeResponse = autocompleteCall()

        override suspend fun geocode(
            text: String,
            size: Int,
        ): GeocodeResponse = geocodeCall()

        override suspend fun directions(request: DirectionsRequest): DirectionsResponse = directionsCall()
    }
}
