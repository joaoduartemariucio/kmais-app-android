package br.com.joaoduartemariucio.rodaro.viewmodels

import br.com.joaoduartemariucio.rodaro.domain.common.errors.AccountError
import br.com.joaoduartemariucio.rodaro.domain.common.services.Account
import br.com.joaoduartemariucio.rodaro.fakes.FakeAccountService
import br.com.joaoduartemariucio.rodaro.presentation.profile.ProfileViewModel
import br.com.joaoduartemariucio.rodaro.presentation.profile.ProfileViewModel.DeleteState
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfileViewModelTests {
    private var cleared = 0

    private fun passwordAccount() = Account(id = "password-id", email = "driver@example.com", displayName = null, provider = Account.Provider.PASSWORD)

    private fun googleAccount() = Account(id = "google-id", email = "driver@gmail.com", displayName = "Driver", provider = Account.Provider.GOOGLE)

    private fun makeViewModel(service: FakeAccountService = FakeAccountService()) = ProfileViewModel(service) { cleared++ }

    @Test
    fun `loads the signed in account`() =
        runTest {
            val service = FakeAccountService(passwordAccount())
            val viewModel = makeViewModel(service)

            viewModel.refresh()

            assertEquals(passwordAccount(), viewModel.account)
            assertEquals(listOf("current"), service.calls)
        }

    @Test
    fun `treats a failed account read as signed out`() =
        runTest {
            val service = FakeAccountService(passwordAccount()).apply { currentError = IllegalStateException("boom") }
            val viewModel = makeViewModel(service)

            viewModel.refresh()

            assertNull(viewModel.account)
        }

    @Test
    fun `signs out and refreshes the account`() =
        runTest {
            val service = FakeAccountService(passwordAccount())
            val viewModel = makeViewModel(service)
            viewModel.refresh()
            assertEquals(passwordAccount(), viewModel.account)

            viewModel.signOut()

            assertNull(viewModel.account)
            assertTrue("signOut" in service.calls)
            assertNull(viewModel.error)
        }

    @Test
    fun `reports a failed sign out`() =
        runTest {
            val failure = IllegalStateException("offline")
            val service = FakeAccountService(passwordAccount()).apply { signOutError = failure }
            val viewModel = makeViewModel(service)
            viewModel.refresh()

            viewModel.signOut()

            assertEquals(failure, viewModel.error)
            viewModel.dismissError()
            assertNull(viewModel.error)
        }

    @Test
    fun `deletes the account and clears the local data`() =
        runTest {
            val service = FakeAccountService(passwordAccount())
            val viewModel = makeViewModel(service)
            viewModel.refresh()

            viewModel.delete()

            assertEquals(1, cleared)
            assertEquals(DeleteState.Done, viewModel.deleteState)
            assertNull(viewModel.account)
            assertTrue("deleteAccount" in service.calls)
            viewModel.dismissDeleteResult()
            assertEquals(DeleteState.Idle, viewModel.deleteState)
        }

    @Test
    fun `asks for the password when Firebase wants a recent login`() =
        runTest {
            val service = FakeAccountService(passwordAccount()).apply { deleteError = AccountError.RecentLoginRequired }
            val viewModel = makeViewModel(service)

            viewModel.delete()
            assertEquals(DeleteState.NeedsPassword, viewModel.deleteState)
            assertEquals(0, cleared)

            service.deleteError = null
            viewModel.confirmWithPassword("secret1")

            assertEquals(DeleteState.Done, viewModel.deleteState)
            assertEquals(1, cleared)
            assertTrue("reauthenticateWithPassword" in service.calls)
        }

    @Test
    fun `asks for Google when the account came from Google`() =
        runTest {
            val service = FakeAccountService(googleAccount()).apply { deleteError = AccountError.RecentLoginRequired }
            val viewModel = makeViewModel(service)
            viewModel.refresh()

            viewModel.delete()
            assertEquals(DeleteState.NeedsGoogle, viewModel.deleteState)

            service.deleteError = null
            viewModel.confirmWithGoogle()

            assertEquals(DeleteState.Done, viewModel.deleteState)
            assertTrue("reauthenticateWithGoogle" in service.calls)
            assertEquals(1, cleared)
        }

    @Test
    fun `reports a failed reauthentication without clearing anything`() =
        runTest {
            val service = FakeAccountService(passwordAccount()).apply { deleteError = AccountError.RecentLoginRequired }
            val viewModel = makeViewModel(service)
            viewModel.delete()
            val failure = AccountError.InvalidCredentials
            service.reauthPasswordError = failure

            viewModel.confirmWithPassword("wrong")

            assertEquals(failure, viewModel.error)
            assertEquals(DeleteState.Idle, viewModel.deleteState)
            assertEquals(0, cleared)
        }

    @Test
    fun `reports a failed delete without touching the local data`() =
        runTest {
            val failure = AccountError.Network
            val service = FakeAccountService(passwordAccount()).apply { deleteError = failure }
            val viewModel = makeViewModel(service)

            viewModel.delete()

            assertEquals(failure, viewModel.error)
            assertEquals(DeleteState.Idle, viewModel.deleteState)
            assertEquals(0, cleared)
        }

    @Test
    fun `leaves the delete flow when the dialog is cancelled`() =
        runTest {
            val service = FakeAccountService(passwordAccount()).apply { deleteError = AccountError.RecentLoginRequired }
            val viewModel = makeViewModel(service)
            viewModel.refresh()

            viewModel.delete()
            assertEquals(DeleteState.NeedsPassword, viewModel.deleteState)

            viewModel.cancelDelete()

            assertEquals(DeleteState.Idle, viewModel.deleteState)
        }
}
