package br.com.joaoduartemariucio.rodaro.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import br.com.joaoduartemariucio.rodaro.domain.preferences.entities.CurrencyOption
import br.com.joaoduartemariucio.rodaro.domain.preferences.entities.LanguageOption
import br.com.joaoduartemariucio.rodaro.domain.preferences.entities.Preferences
import br.com.joaoduartemariucio.rodaro.domain.preferences.entities.UnitSystem
import br.com.joaoduartemariucio.rodaro.domain.preferences.repositories.PreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SharedPreferencesPreferencesRepository(
    context: Context,
) : PreferencesRepository {
    private val storage: SharedPreferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    override suspend fun get(): Preferences =
        withContext(Dispatchers.IO) {
            Preferences(
                pushEnabled = storage.getBoolean(KEY_PUSH, true),
                remindersEnabled = storage.getBoolean(KEY_REMINDERS, true),
                currency = storage.getString(KEY_CURRENCY, null)?.let(::currency),
                unitSystem = storage.getString(KEY_UNITS, null)?.let(::units) ?: UnitSystem.METRIC,
                language = storage.getString(KEY_LANGUAGE, null)?.let(::language) ?: LanguageOption.SYSTEM,
            )
        }

    override suspend fun save(preferences: Preferences) =
        withContext(Dispatchers.IO) {
            storage.edit {
                putBoolean(KEY_PUSH, preferences.pushEnabled)
                putBoolean(KEY_REMINDERS, preferences.remindersEnabled)
                putString(KEY_CURRENCY, preferences.currency?.name)
                putString(KEY_UNITS, preferences.unitSystem.name)
                putString(KEY_LANGUAGE, preferences.language.name)
            }
        }

    private fun currency(name: String): CurrencyOption? = CurrencyOption.entries.firstOrNull { it.name == name }

    private fun units(name: String): UnitSystem? = UnitSystem.entries.firstOrNull { it.name == name }

    private fun language(name: String): LanguageOption? = LanguageOption.entries.firstOrNull { it.name == name }

    companion object {
        const val FILE_NAME = "preferences"
        const val KEY_PUSH = "push_enabled"
        const val KEY_REMINDERS = "reminders_enabled"
        const val KEY_CURRENCY = "currency"
        const val KEY_UNITS = "units"
        const val KEY_LANGUAGE = "language"
    }
}
