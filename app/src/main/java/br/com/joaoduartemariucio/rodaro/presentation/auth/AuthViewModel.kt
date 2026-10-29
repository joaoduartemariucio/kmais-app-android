package br.com.joaoduartemariucio.rodaro.presentation.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import br.com.joaoduartemariucio.rodaro.app.NoAnalytics
import br.com.joaoduartemariucio.rodaro.domain.common.errors.AccountError
import br.com.joaoduartemariucio.rodaro.domain.common.services.AccountService
import br.com.joaoduartemariucio.rodaro.domain.common.services.Analytics
import kotlin.coroutines.cancellation.CancellationException

class AuthViewModel(
    private val accountService: AccountService,
    private val analytics: Analytics = NoAnalytics,
) : ViewModel() {
    enum class Mode {
        SignIn,
        CreateAccount,
    }

    sealed interface Input {
        data object EmailRequired : Input

        data object EmailInvalid : Input

        data object PasswordRequired : Input

        data object PasswordTooShort : Input
    }

    var mode: Mode by mutableStateOf(Mode.SignIn)
        private set

    var email by mutableStateOf("")

    var password by mutableStateOf("")

    var working by mutableStateOf(false)
        private set

    var error: Throwable? by mutableStateOf(null)
        private set

    val inputs: List<Input>
        get() =
            buildList {
                when {
                    email.isBlank() -> add(Input.EmailRequired)
                    !EMAIL.matches(email.trim()) -> add(Input.EmailInvalid)
                }
                when {
                    password.isEmpty() -> add(Input.PasswordRequired)
                    password.length < MIN_PASSWORD -> add(Input.PasswordTooShort)
                }
            }

    val canSubmit: Boolean
        get() = inputs.isEmpty() && !working

    suspend fun submit(): Boolean =
        if (canSubmit) {
            perform {
                if (mode == Mode.SignIn) {
                    accountService.signIn(email.trim(), password)
                } else {
                    accountService.createAccount(email.trim(), password)
                }
                analytics.event(
                    if (mode == Mode.SignIn) "auth_signed_in" else "auth_account_created",
                    mapOf("method" to "email_password"),
                )
            }
        } else {
            false
        }

    suspend fun submitWithGoogle(): Boolean =
        if (working) {
            false
        } else {
            perform {
                accountService.signInWithGoogle()
                analytics.event("auth_signed_in", mapOf("method" to "google"))
            }
        }

    fun switchMode() {
        mode = if (mode == Mode.SignIn) Mode.CreateAccount else Mode.SignIn
        error = null
    }

    fun dismissError() {
        error = null
    }

    private suspend fun perform(action: suspend () -> Unit): Boolean {
        error = null
        working = true
        return try {
            action()
            true
        } catch (failure: Exception) {
            if (failure is CancellationException) throw failure
            if (failure !is AccountError.Cancelled) error = failure
            false
        } finally {
            working = false
        }
    }

    companion object {
        private const val MIN_PASSWORD = 6

        private val EMAIL = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}
