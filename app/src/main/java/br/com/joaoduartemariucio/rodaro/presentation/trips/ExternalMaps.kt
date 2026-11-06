package br.com.joaoduartemariucio.rodaro.presentation.trips

import br.com.joaoduartemariucio.rodaro.domain.trips.entities.RoutePoint
import java.net.URLEncoder

object ExternalMaps {
    fun googleMaps(
        origin: String,
        destination: String,
    ): String = "https://www.google.com/maps/dir/?api=1&origin=${encode(origin)}&destination=${encode(destination)}&travelmode=driving"

    fun waze(
        destination: String,
        end: RoutePoint?,
    ): String =
        end?.let { "https://waze.com/ul?ll=${it.latitude},${it.longitude}&navigate=yes" }
            ?: "https://waze.com/ul?q=${encode(destination)}&navigate=yes"

    private fun encode(text: String) = URLEncoder.encode(text.trim(), Charsets.UTF_8).replace("+", "%20")
}
