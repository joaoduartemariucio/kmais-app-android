package br.com.joaoduartemariucio.kmais_app_android.data.services

import br.com.joaoduartemariucio.kmais_app_android.domain.services.DateProvider
import java.time.Instant
import java.time.ZoneId

class SystemDateProvider : DateProvider {
    override val now: Instant get() = Instant.now()
    override val zone: ZoneId get() = ZoneId.systemDefault()
}
