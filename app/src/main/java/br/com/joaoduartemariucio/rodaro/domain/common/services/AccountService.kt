package br.com.joaoduartemariucio.rodaro.domain.common.services

interface AccountService {
    suspend fun current(): Account?

    suspend fun signIn(
        email: String,
        password: String,
    )

    suspend fun createAccount(
        email: String,
        password: String,
    )

    suspend fun signInWithGoogle()

    suspend fun signOut()

    suspend fun deleteAccount()

    suspend fun reauthenticateWithPassword(password: String)

    suspend fun reauthenticateWithGoogle()
}
