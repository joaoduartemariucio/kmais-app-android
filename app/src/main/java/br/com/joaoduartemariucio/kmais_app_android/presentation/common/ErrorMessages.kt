package br.com.joaoduartemariucio.kmais_app_android.presentation.common

import br.com.joaoduartemariucio.kmais_app_android.R
import br.com.joaoduartemariucio.kmais_app_android.domain.errors.RepositoryError

fun Throwable.message(localizer: Localizer): String =
    when (this) {
        is RepositoryError.VehicleNotFound -> localizer.string(R.string.error_vehicle_not_found)
        is RepositoryError.EntryNotFound -> localizer.string(R.string.error_entry_not_found)
        is RepositoryError.PersistenceFailed -> localizer.string(R.string.error_persistence_failed)
        else -> localizedMessage ?: localizer.string(R.string.error_persistence_failed)
    }
