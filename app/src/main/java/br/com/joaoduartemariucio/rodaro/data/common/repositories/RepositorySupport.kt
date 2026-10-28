package br.com.joaoduartemariucio.rodaro.data.common.repositories

import br.com.joaoduartemariucio.rodaro.domain.common.errors.RepositoryError
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
