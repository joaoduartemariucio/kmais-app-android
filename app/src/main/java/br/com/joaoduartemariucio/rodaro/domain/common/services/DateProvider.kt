package br.com.joaoduartemariucio.rodaro.domain.common.services

import java.time.Instant
import java.time.ZoneId

interface DateProvider {
    val now: Instant
    val zone: ZoneId
}
