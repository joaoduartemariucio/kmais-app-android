package br.com.joaoduartemariucio.rodaro.data.common.services

import br.com.joaoduartemariucio.rodaro.domain.common.errors.AccountError

fun accountError(
    code: String?,
    cause: Throwable? = null,
): AccountError =
    when (code?.removePrefix("ERROR_")) {
        "INVALID_EMAIL", "MISSING_EMAIL" -> AccountError.InvalidEmail
        "INVALID_PASSWORD", "WRONG_PASSWORD" -> AccountError.InvalidCredentials
        "INVALID_LOGIN_CREDENTIALS", "INVALID_CREDENTIAL" -> AccountError.InvalidCredentials
        "INVALID_IDP_RESPONSE", "MISSING_PASSWORD" -> AccountError.InvalidCredentials
        "USER_NOT_FOUND", "USER_DISABLED" -> AccountError.InvalidCredentials
        "EMAIL_ALREADY_IN_USE", "EMAIL_EXISTS" -> AccountError.EmailInUse
        "WEAK_PASSWORD", "WEAK_PASSWORD_AT_LEAST_6_CHARACTERS" -> AccountError.WeakPassword
        "TOO_MANY_REQUESTS" -> AccountError.TooManyRequests
        "NETWORK_REQUEST_FAILED" -> AccountError.Network
        "REQUIRES_RECENT_LOGIN" -> AccountError.RecentLoginRequired
        "OPERATION_NOT_ALLOWED" -> AccountError.OperationNotAllowed
        "CREDENTIAL_ALREADY_IN_USE" -> AccountError.InvalidCredentials
        else -> AccountError.Failed(cause ?: IllegalStateException(code ?: "unknown auth failure"))
    }
