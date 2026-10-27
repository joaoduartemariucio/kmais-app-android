package br.com.joaoduartemariucio.kmais_app_android.domain.entities

import java.time.Instant

interface Chronological {
    val date: Instant
    val odometer: Double
}

fun <T : Chronological> List<T>.sortedChronologically(): List<T> = sortedWith(compareBy<T>({ it.date }, { it.odometer }))
