package br.com.joaoduartemariucio.kmais_app_android.data.repositories

import br.com.joaoduartemariucio.kmais_app_android.domain.errors.RepositoryError
import kotlin.coroutines.cancellation.CancellationException

internal suspend fun <T> persisting(block: suspend () -> T): T =
    try {
        block()
    } catch (error: RepositoryError) {
        throw error
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        throw RepositoryError.PersistenceFailed(error)
    }
