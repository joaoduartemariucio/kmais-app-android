package br.com.joaoduartemariucio.rodaro.data.network

import br.com.joaoduartemariucio.rodaro.domain.trips.entities.RoutePoint
import kotlin.math.pow
import kotlin.math.roundToLong

object Polyline {
    const val PRECISION_5 = 5
    const val PRECISION_6 = 6

    fun decode(
        encoded: String,
        precision: Int,
    ): List<RoutePoint> {
        val factor = 10.0.pow(precision)
        val points = mutableListOf<RoutePoint>()
        var index = 0
        var latitude = 0L
        var longitude = 0L
        while (index < encoded.length) {
            val (latitudeDelta, afterLatitude) = value(encoded, index) ?: break
            val (longitudeDelta, afterLongitude) = value(encoded, afterLatitude) ?: break
            latitude += latitudeDelta
            longitude += longitudeDelta
            index = afterLongitude
            points += RoutePoint(latitude / factor, longitude / factor)
        }
        return points
    }

    fun encode(
        points: List<RoutePoint>,
        precision: Int,
    ): String {
        val factor = 10.0.pow(precision)
        val builder = StringBuilder()
        var latitude = 0L
        var longitude = 0L
        for (point in points) {
            val nextLatitude = (point.latitude * factor).roundToLong()
            val nextLongitude = (point.longitude * factor).roundToLong()
            append(builder, nextLatitude - latitude)
            append(builder, nextLongitude - longitude)
            latitude = nextLatitude
            longitude = nextLongitude
        }
        return builder.toString()
    }

    private fun value(
        encoded: String,
        start: Int,
    ): Pair<Long, Int>? {
        var result = 0L
        var shift = 0
        var index = start
        while (index < encoded.length) {
            val chunk = encoded[index++].code - ASCII_OFFSET
            result = result or ((chunk and CHUNK_MASK).toLong() shl shift)
            shift += CHUNK_BITS
            if (chunk < CONTINUATION) {
                val delta = if (result and 1L != 0L) (result shr 1).inv() else result shr 1
                return delta to index
            }
        }
        return null
    }

    private fun append(
        builder: StringBuilder,
        delta: Long,
    ) {
        var value = if (delta < 0) (delta shl 1).inv() else delta shl 1
        while (value >= CONTINUATION) {
            builder.append(((CONTINUATION or (value and CHUNK_MASK.toLong()).toInt()) + ASCII_OFFSET).toChar())
            value = value shr CHUNK_BITS
        }
        builder.append((value.toInt() + ASCII_OFFSET).toChar())
    }

    private const val ASCII_OFFSET = 63
    private const val CHUNK_BITS = 5
    private const val CHUNK_MASK = 0x1f
    private const val CONTINUATION = 0x20
}
