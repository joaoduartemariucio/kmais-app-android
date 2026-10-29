package br.com.joaoduartemariucio.rodaro.presentation.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import br.com.joaoduartemariucio.rodaro.domain.common.errors.AccountError
import br.com.joaoduartemariucio.rodaro.domain.common.services.Account
import br.com.joaoduartemariucio.rodaro.domain.common.services.AccountService
import kotlin.coroutines.cancellation.CancellationException

class ProfileViewModel(
    private val accountService: AccountService,
    private val clearLocalData: suspend () -> Unit,
) : ViewModel() {
    sealed interface DeleteState {
        data object Idle : DeleteState

        data object Working : DeleteState

        data object NeedsPassword : DeleteState

        data object NeedsGoogle : DeleteState

        data object Done : DeleteState
    }

    var account: Account? by mutableStateOf(null)
        private set

    var deleteState: DeleteState by mutableStateOf(DeleteState.Idle)
        private set

    var error: Throwable? by mutableStateOf(null)
        private set

    suspend fun refresh() {
        account =
            try {
                accountService.current()
            } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                null
            }
    }

    suspend fun signOut() {
        error = null
        try {
            accountService.signOut()
        } catch (failure: Exception) {
            if (failure is CancellationException) throw failure
            error = failure
        }
        refresh()
    }

    suspend fun delete() = performDelete { accountService.deleteAccount() }

    suspend fun confirmWithPassword(password: String) =
        performDelete {
            accountService.reauthenticateWithPassword(password)
            accountService.deleteAccount()
        }

    suspend fun confirmWithGoogle() =
        performDelete {
            accountService.reauthenticateWithGoogle()
            accountService.deleteAccount()
        }

    fun cancelDelete() {
        if (deleteState != DeleteState.Working) deleteState = DeleteState.Idle
    }

    fun dismissDeleteResult() {
        if (deleteState == DeleteState.Done) deleteState = DeleteState.Idle
    }

    fun dismissError() {
        error = null
    }

    private suspend fun performDelete(action: suspend () -> Unit) {
        if (deleteState == DeleteState.Working) return
        deleteState = DeleteState.Working
        error = null
        try {
            action()
            clearLocalData()
            refresh()
            deleteState = DeleteState.Done
        } catch (recentLogin: AccountError.RecentLoginRequired) {
            deleteState =
                if (account?.provider == Account.Provider.GOOGLE) {
                    DeleteState.NeedsGoogle
                } else {
                    DeleteState.NeedsPassword
                }
        } catch (failure: Exception) {
            if (failure is CancellationException) throw failure
            error = failure
            deleteState = DeleteState.Idle
        }
    }
}
