package br.com.joaoduartemariucio.rodaro.app

import br.com.joaoduartemariucio.rodaro.domain.common.services.Analytics

object NoAnalytics : Analytics {
    override fun screen(name: String) = Unit

    override fun event(
        name: String,
        parameters: Map<String, String>,
    ) = Unit
}
