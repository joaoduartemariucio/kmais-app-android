package br.com.joaoduartemariucio.rodaro.data

import br.com.joaoduartemariucio.rodaro.data.common.services.accountError
import br.com.joaoduartemariucio.rodaro.domain.common.errors.AccountError
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AuthErrorsTests {
    private val known =
        mapOf(
            "INVALID_EMAIL" to AccountError.InvalidEmail,
            "MISSING_EMAIL" to AccountError.InvalidEmail,
            "INVALID_PASSWORD" to AccountError.InvalidCredentials,
            "WRONG_PASSWORD" to AccountError.InvalidCredentials,
            "INVALID_LOGIN_CREDENTIALS" to AccountError.InvalidCredentials,
            "INVALID_CREDENTIAL" to AccountError.InvalidCredentials,
            "INVALID_IDP_RESPONSE" to AccountError.InvalidCredentials,
            "MISSING_PASSWORD" to AccountError.InvalidCredentials,
            "USER_NOT_FOUND" to AccountError.InvalidCredentials,
            "USER_DISABLED" to AccountError.InvalidCredentials,
            "CREDENTIAL_ALREADY_IN_USE" to AccountError.InvalidCredentials,
            "EMAIL_ALREADY_IN_USE" to AccountError.EmailInUse,
            "EMAIL_EXISTS" to AccountError.EmailInUse,
            "WEAK_PASSWORD" to AccountError.WeakPassword,
            "WEAK_PASSWORD_AT_LEAST_6_CHARACTERS" to AccountError.WeakPassword,
            "TOO_MANY_REQUESTS" to AccountError.TooManyRequests,
            "NETWORK_REQUEST_FAILED" to AccountError.Network,
            "REQUIRES_RECENT_LOGIN" to AccountError.RecentLoginRequired,
            "OPERATION_NOT_ALLOWED" to AccountError.OperationNotAllowed,
        )

    @Test
    fun `maps every known Firebase auth code to its account error`() {
        for ((code, expected) in known) {
            assertEquals(expected, accountError(code), code)
        }
    }

    @Test
    fun `accepts the codes with the ERROR prefix`() {
        for ((code, expected) in known) {
            assertEquals(expected, accountError("ERROR_$code"), code)
        }
    }

    @Test
    fun `falls back to a failed error for an unknown code`() {
        val cause = IllegalStateException("boom")
        assertEquals(AccountError.Failed(cause), accountError("SOMETHING_NEW", cause))

        val fallback = accountError(null)
        assertIs<AccountError.Failed>(fallback)
        assertEquals("unknown auth failure", fallback.reason.message)
    }
}
