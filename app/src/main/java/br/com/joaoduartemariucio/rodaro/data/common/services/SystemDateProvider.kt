package br.com.joaoduartemariucio.rodaro.data.common.services

import br.com.joaoduartemariucio.rodaro.domain.common.services.DateProvider
import java.time.Instant
import java.time.ZoneId

class SystemDateProvider : DateProvider {
    override val now: Instant get() = Instant.now()
    override val zone: ZoneId get() = ZoneId.systemDefault()
}
