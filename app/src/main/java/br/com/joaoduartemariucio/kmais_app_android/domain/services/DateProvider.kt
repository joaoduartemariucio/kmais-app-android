package br.com.joaoduartemariucio.kmais_app_android.domain.services

import java.time.Instant
import java.time.ZoneId

interface DateProvider {
    val now: Instant
    val zone: ZoneId
}
