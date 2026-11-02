package br.com.joaoduartemariucio.rodaro.domain.common.errors

sealed class RouteLookupError(
    cause: Throwable? = null,
) : Exception(cause) {
    data object NoApiKey : RouteLookupError()

    data object InvalidApiKey : RouteLookupError()

    data object QuotaExceeded : RouteLookupError()

    data object NetworkUnavailable : RouteLookupError()

    data class PlaceNotFound(
        val text: String,
    ) : RouteLookupError()

    class RequestFailed(
        underlying: Throwable,
    ) : RouteLookupError(underlying)
}
