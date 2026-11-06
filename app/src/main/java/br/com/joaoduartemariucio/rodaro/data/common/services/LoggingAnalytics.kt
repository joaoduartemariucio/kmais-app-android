package br.com.joaoduartemariucio.rodaro.data.common.services

import br.com.joaoduartemariucio.rodaro.domain.common.services.Analytics

class LoggingAnalytics(
    private val wrapped: Analytics,
    private val log: (String) -> Unit,
) : Analytics {
    override fun screen(name: String) {
        log("screen $name")
        wrapped.screen(name)
    }

    override fun event(
        name: String,
        parameters: Map<String, String>,
    ) {
        log(if (parameters.isEmpty()) "event $name" else "event $name $parameters")
        wrapped.event(name, parameters)
    }
}
