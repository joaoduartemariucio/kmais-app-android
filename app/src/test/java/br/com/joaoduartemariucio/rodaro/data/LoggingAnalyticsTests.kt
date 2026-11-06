package br.com.joaoduartemariucio.rodaro.data

import br.com.joaoduartemariucio.rodaro.data.common.services.LoggingAnalytics
import br.com.joaoduartemariucio.rodaro.domain.common.services.Analytics
import org.junit.Test
import kotlin.test.assertEquals

class LoggingAnalyticsTests {
    private val lines = mutableListOf<String>()
    private val forwarded = mutableListOf<String>()

    private val wrapped =
        object : Analytics {
            override fun screen(name: String) {
                forwarded += "screen:$name"
            }

            override fun event(
                name: String,
                parameters: Map<String, String>,
            ) {
                forwarded += "event:$name:$parameters"
            }
        }

    private val analytics = LoggingAnalytics(wrapped, lines::add)

    @Test
    fun `screens and events are logged and still reach the real tracker`() {
        analytics.screen("trip_cost")
        analytics.event("route_calculated")
        analytics.event("route_saved", mapOf("round_trip" to "true"))

        assertEquals(listOf("screen trip_cost", "event route_calculated", "event route_saved {round_trip=true}"), lines)
        assertEquals(
            listOf("screen:trip_cost", "event:route_calculated:{}", "event:route_saved:{round_trip=true}"),
            forwarded,
        )
    }
}
