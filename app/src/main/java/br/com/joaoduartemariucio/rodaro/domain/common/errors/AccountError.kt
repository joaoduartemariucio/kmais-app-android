package br.com.joaoduartemariucio.rodaro.domain.common.errors

sealed class AccountError(
    cause: Throwable? = null,
) : Exception(cause) {
    data object InvalidEmail : AccountError()

    data object InvalidCredentials : AccountError()

    data object EmailInUse : AccountError()

    data object WeakPassword : AccountError()

    data object TooManyRequests : AccountError()

    data object Network : AccountError()

    data object RecentLoginRequired : AccountError()

    data object OperationNotAllowed : AccountError()

    data object GoogleNotConfigured : AccountError()

    data object NoGoogleAccount : AccountError()

    data object Cancelled : AccountError()

    data object NotSignedIn : AccountError()

    data class Failed(
        val reason: Throwable,
    ) : AccountError(reason)
}
