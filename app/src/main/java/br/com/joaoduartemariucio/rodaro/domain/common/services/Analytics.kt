package br.com.joaoduartemariucio.rodaro.domain.common.services

interface Analytics {
    fun screen(name: String)

    fun event(
        name: String,
        parameters: Map<String, String> = emptyMap(),
    )
}
