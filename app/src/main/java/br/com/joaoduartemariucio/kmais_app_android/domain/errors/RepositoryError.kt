package br.com.joaoduartemariucio.kmais_app_android.domain.errors

import java.util.UUID

sealed class RepositoryError(
    cause: Throwable? = null,
) : Exception(cause) {
    data class VehicleNotFound(
        val id: UUID,
    ) : RepositoryError()

    data class EntryNotFound(
        val id: UUID,
    ) : RepositoryError()

    class PersistenceFailed(
        underlying: Throwable,
    ) : RepositoryError(underlying)
}
