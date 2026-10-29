package br.com.joaoduartemariucio.rodaro.viewmodels

import br.com.joaoduartemariucio.rodaro.domain.common.errors.AccountError
import br.com.joaoduartemariucio.rodaro.fakes.FakeAccountService
import br.com.joaoduartemariucio.rodaro.presentation.auth.AuthViewModel
import br.com.joaoduartemariucio.rodaro.presentation.auth.AuthViewModel.Input
import br.com.joaoduartemariucio.rodaro.presentation.auth.AuthViewModel.Mode
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthViewModelTests {
    private fun makeViewModel(service: FakeAccountService = FakeAccountService()) = AuthViewModel(service)

    private fun AuthViewModel.fillValidFields() {
        email = " driver@example.com "
        password = "secret1"
    }

    @Test
    fun `asks for an e-mail and a password while both fields are empty`() =
        runTest {
            val inputs = makeViewModel().inputs
            assertEquals(listOf(Input.EmailRequired, Input.PasswordRequired), inputs)
        }

    @Test
    fun `rejects a malformed e-mail and a short password`() {
        val viewModel = makeViewModel()
        viewModel.email = "driver@"
        viewModel.password = "123"
        assertEquals(listOf(Input.EmailInvalid, Input.PasswordTooShort), viewModel.inputs)
    }

    @Test
    fun `accepts a valid e-mail and password`() {
        val viewModel = makeViewModel()
        viewModel.fillValidFields()
        assertEquals(emptyList(), viewModel.inputs)
        assertTrue(viewModel.canSubmit)
    }

    @Test
    fun `does not call the service while the inputs are invalid`() =
        runTest {
            val service = FakeAccountService()
            val viewModel = makeViewModel(service)
            assertFalse(viewModel.submit())
            assertEquals(emptyList(), service.calls)
        }

    @Test
    fun `signs in with the trimmed e-mail and password`() =
        runTest {
            val service = FakeAccountService()
            val viewModel = makeViewModel(service)
            viewModel.fillValidFields()

            assertTrue(viewModel.submit())

            assertEquals(listOf("signIn:driver@example.com:secret1"), service.calls)
            assertNull(viewModel.error)
            assertFalse(viewModel.working)
        }

    @Test
    fun `creates an account when the mode is create`() =
        runTest {
            val service = FakeAccountService()
            val viewModel = makeViewModel(service)
            viewModel.switchMode()
            viewModel.fillValidFields()

            assertTrue(viewModel.submit())

            assertEquals(Mode.CreateAccount, viewModel.mode)
            assertEquals(listOf("createAccount:driver@example.com:secret1"), service.calls)
        }

    @Test
    fun `keeps the form open and reports a failed sign in`() =
        runTest {
            val failure = IllegalStateException("boom")
            val service = FakeAccountService().apply { signInError = failure }
            val viewModel = makeViewModel(service)
            viewModel.fillValidFields()

            assertFalse(viewModel.submit())

            assertEquals(failure, viewModel.error)
            assertFalse(viewModel.working)
        }

    @Test
    fun `switching mode or dismissing clears the previous error`() =
        runTest {
            val service = FakeAccountService().apply { signInError = AccountError.InvalidCredentials }
            val viewModel = makeViewModel(service)
            viewModel.fillValidFields()
            viewModel.submit()
            assertNotNull(viewModel.error)

            viewModel.switchMode()
            assertNull(viewModel.error)
            assertEquals(Mode.CreateAccount, viewModel.mode)

            service.createAccountError = AccountError.EmailInUse
            viewModel.submit()
            assertNotNull(viewModel.error)
            viewModel.dismissError()
            assertNull(viewModel.error)
            assertEquals(Mode.CreateAccount, viewModel.mode)
        }

    @Test
    fun `signs in with Google`() =
        runTest {
            val service = FakeAccountService()
            val viewModel = makeViewModel(service)

            assertTrue(viewModel.submitWithGoogle())

            assertEquals(listOf("signInWithGoogle"), service.calls)
            assertNull(viewModel.error)
        }

    @Test
    fun `reports a failed Google sign in`() =
        runTest {
            val failure = AccountError.NoGoogleAccount
            val service = FakeAccountService().apply { signInWithGoogleError = failure }
            val viewModel = makeViewModel(service)

            assertFalse(viewModel.submitWithGoogle())

            assertEquals(failure, viewModel.error)
        }

    @Test
    fun `stays quiet when the person cancels the Google dialog`() =
        runTest {
            val service = FakeAccountService().apply { signInWithGoogleError = AccountError.Cancelled }
            val viewModel = makeViewModel(service)

            assertFalse(viewModel.submitWithGoogle())

            assertNull(viewModel.error)
        }
}
