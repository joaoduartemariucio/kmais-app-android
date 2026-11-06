package br.com.joaoduartemariucio.rodaro.data

import br.com.joaoduartemariucio.rodaro.data.network.HttpLogInterceptor
import br.com.joaoduartemariucio.rodaro.data.network.OrsNetwork
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HttpLogInterceptorTests {
    private lateinit var server: MockWebServer
    private val blocks = mutableListOf<String>()

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
    fun `a request and its response are logged with headers, pretty json and the elapsed time`() {
        var now = 0L
        val client =
            OkHttpClient
                .Builder()
                .addInterceptor(HttpLogInterceptor(blocks::add) { now.also { now += 42_000_000 } })
                .build()
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"features":[{"label":"Campinas"}]}"""),
        )

        client
            .newCall(
                Request
                    .Builder()
                    .url(server.url("/v2/directions?api_key=secret&profile=car"))
                    .header("Accept", "application/json")
                    .post("""{"coordinates":[[1,2]]}""".toRequestBody("application/json".toMediaType()))
                    .build(),
            ).execute()
            .use { assertEquals("""{"features":[{"label":"Campinas"}]}""", it.body?.string()) }

        val (request, response) = blocks
        val url = "http://${server.hostName}:${server.port}/v2/directions?profile=car"
        assertTrue(request.startsWith("┌ #1 → POST $url"))
        assertTrue("│   Accept: application/json" in request)
        assertTrue("│   Content-Type: application/json; charset=utf-8" in request)
        assertTrue("│   {\n│       \"coordinates\": [" in request)
        assertTrue(response.startsWith("┌ #1 ← 200 OK POST $url in 42 ms"))
        assertTrue("│   Content-Type: application/json" in response)
        assertTrue("│           {\n│               \"label\": \"Campinas\"" in response)
        assertTrue(response.endsWith("\n└"))
    }

    @Test
    fun `a request without body and an empty response say so`() {
        val client = OkHttpClient.Builder().addInterceptor(HttpLogInterceptor(blocks::add)).build()
        server.enqueue(MockResponse().setResponseCode(204))

        client.newCall(Request.Builder().url(server.url("/ping")).build()).execute().close()

        assertFalse("Body" in blocks[0])
        assertTrue("│ Body: (empty)" in blocks[1])
    }

    @Test
    fun `binary and very long bodies are summarized`() {
        val client = OkHttpClient.Builder().addInterceptor(HttpLogInterceptor(blocks::add)).build()
        server.enqueue(MockResponse().setHeader("Content-Type", "image/png").setBody("PNGDATA"))
        server.enqueue(MockResponse().setHeader("Content-Type", "text/plain").setBody("x".repeat(3_000)))

        client.newCall(Request.Builder().url(server.url("/tile.png")).build()).execute().close()
        client.newCall(Request.Builder().url(server.url("/text")).build()).execute().close()

        assertTrue("(7 bytes of image/png)" in blocks[1])
        assertTrue("… 500 more chars" in blocks[3])
    }

    @Test
    fun `a failed connection is logged and rethrown`() {
        val client = OkHttpClient.Builder().addInterceptor(HttpLogInterceptor(blocks::add)).build()
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        assertFailsWith<IOException> {
            client.newCall(Request.Builder().url(server.url("/v2/directions")).build()).execute()
        }

        assertTrue(blocks.last().startsWith("┌ #1 ✕ FAILED GET http://${server.hostName}:${server.port}/v2/directions after"))
    }

    @Test
    fun `the ors client logs the real headers but never the api key`() =
        runTest {
            server.enqueue(MockResponse().setHeader("Set-Cookie", "session=abc").setBody("""{"features":[]}"""))
            val api = OrsNetwork.api("secret-key", server.url("/").toString(), log = blocks::add)

            api.geocode("Campinas")

            assertTrue("/geocode/search?text=Campinas&size=1" in blocks[0])
            assertTrue("│   Authorization: ██ (hidden)" in blocks[0])
            assertTrue("│   Set-Cookie: ██ (hidden)" in blocks[1])
            assertFalse(blocks.any { "secret-key" in it || "session=abc" in it })
            assertEquals("secret-key", server.takeRequest().getHeader("Authorization"))
        }
}
