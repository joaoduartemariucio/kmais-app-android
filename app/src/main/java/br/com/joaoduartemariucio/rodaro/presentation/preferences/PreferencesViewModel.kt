package br.com.joaoduartemariucio.rodaro.presentation.preferences

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import br.com.joaoduartemariucio.rodaro.app.NoAnalytics
import br.com.joaoduartemariucio.rodaro.domain.common.services.Analytics
import br.com.joaoduartemariucio.rodaro.domain.preferences.entities.CurrencyOption
import br.com.joaoduartemariucio.rodaro.domain.preferences.entities.LanguageOption
import br.com.joaoduartemariucio.rodaro.domain.preferences.entities.Preferences
import br.com.joaoduartemariucio.rodaro.domain.preferences.entities.UnitSystem
import br.com.joaoduartemariucio.rodaro.domain.preferences.repositories.PreferencesRepository
import br.com.joaoduartemariucio.rodaro.presentation.common.DisplayPreferences
import br.com.joaoduartemariucio.rodaro.presentation.common.FormState
import kotlin.coroutines.cancellation.CancellationException

class PreferencesViewModel(
    private val repository: PreferencesRepository,
    private val applyLanguage: (LanguageOption) -> Unit,
    private val syncReminders: suspend () -> Unit,
    private val analytics: Analytics = NoAnalytics,
) : ViewModel() {
    var preferences: Preferences by mutableStateOf(Preferences())
        private set

    var loaded by mutableStateOf(false)
        private set

    var state: FormState by mutableStateOf(FormState.Editing)
        private set

    suspend fun load() {
        try {
            val saved = repository.get()
            preferences = saved
            DisplayPreferences.update(saved)
            loaded = true
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            state = FormState.Failed(error)
        }
    }

    suspend fun setPushEnabled(enabled: Boolean) = save("push_enabled", preferences.copy(pushEnabled = enabled))

    suspend fun setRemindersEnabled(enabled: Boolean) = save("reminders_enabled", preferences.copy(remindersEnabled = enabled))

    suspend fun setCurrency(currency: CurrencyOption?) = save("currency", preferences.copy(currency = currency))

    suspend fun setUnitSystem(unitSystem: UnitSystem) = save("unit_system", preferences.copy(unitSystem = unitSystem))

    suspend fun setLanguage(language: LanguageOption) = save("language", preferences.copy(language = language))

    private suspend fun save(
        key: String,
        updated: Preferences,
    ) {
        val previous = preferences
        preferences = updated
        DisplayPreferences.update(updated)
        state = FormState.Saving
        state =
            try {
                repository.save(updated)
                analytics.event("preference_changed", mapOf("key" to key))
                if (updated.remindersEnabled != previous.remindersEnabled) syncReminders()
                if (updated.language != previous.language) applyLanguage(updated.language)
                FormState.Saved
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                FormState.Failed(error)
            }
    }
}
