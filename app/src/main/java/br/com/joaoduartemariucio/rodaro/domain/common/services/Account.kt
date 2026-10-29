package br.com.joaoduartemariucio.rodaro.domain.common.services

data class Account(
    val id: String,
    val email: String?,
    val displayName: String?,
    val provider: Provider,
) {
    enum class Provider {
        GOOGLE,
        PASSWORD,
    }
}
