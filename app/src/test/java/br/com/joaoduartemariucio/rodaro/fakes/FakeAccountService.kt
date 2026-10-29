package br.com.joaoduartemariucio.rodaro.fakes

import br.com.joaoduartemariucio.rodaro.domain.common.services.Account
import br.com.joaoduartemariucio.rodaro.domain.common.services.AccountService

class FakeAccountService(
    initial: Account? = null,
) : AccountService {
    var account: Account? = initial
        private set

    var signInError: Throwable? = null

    var createAccountError: Throwable? = null

    var signInWithGoogleError: Throwable? = null

    var signOutError: Throwable? = null

    var currentError: Throwable? = null

    var deleteError: Throwable? = null

    var reauthPasswordError: Throwable? = null

    var reauthGoogleError: Throwable? = null

    val calls = mutableListOf<String>()

    override suspend fun current(): Account? {
        calls += "current"
        currentError?.let { throw it }
        return account
    }

    override suspend fun signIn(
        email: String,
        password: String,
    ) {
        calls += "signIn:$email:$password"
        signInError?.let { throw it }
        account = Account(id = "password-id", email = email, displayName = null, provider = Account.Provider.PASSWORD)
    }

    override suspend fun createAccount(
        email: String,
        password: String,
    ) {
        calls += "createAccount:$email:$password"
        createAccountError?.let { throw it }
        account = Account(id = "password-id", email = email, displayName = null, provider = Account.Provider.PASSWORD)
    }

    override suspend fun signInWithGoogle() {
        calls += "signInWithGoogle"
        signInWithGoogleError?.let { throw it }
        account = Account(id = "google-id", email = "driver@gmail.com", displayName = "Driver", provider = Account.Provider.GOOGLE)
    }

    override suspend fun signOut() {
        calls += "signOut"
        signOutError?.let { throw it }
        account = null
    }

    override suspend fun deleteAccount() {
        calls += "deleteAccount"
        deleteError?.let { throw it }
        account = null
    }

    override suspend fun reauthenticateWithPassword(password: String) {
        calls += "reauthenticateWithPassword"
        reauthPasswordError?.let { throw it }
    }

    override suspend fun reauthenticateWithGoogle() {
        calls += "reauthenticateWithGoogle"
        reauthGoogleError?.let { throw it }
        account = Account(id = "google-id", email = "driver@gmail.com", displayName = "Driver", provider = Account.Provider.GOOGLE)
    }
}
