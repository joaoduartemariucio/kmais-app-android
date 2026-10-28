package br.com.joaoduartemariucio.rodaro.presentation.common

sealed interface FormState {
    data object Editing : FormState

    data object Saving : FormState

    data object Saved : FormState

    data class Failed(
        val error: Throwable,
    ) : FormState

    val isSaving: Boolean get() = this == Saving
}
