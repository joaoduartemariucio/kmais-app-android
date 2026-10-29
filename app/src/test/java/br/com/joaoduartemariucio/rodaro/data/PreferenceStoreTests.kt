package br.com.joaoduartemariucio.rodaro.data

import android.content.Context
import android.os.LocaleList
import androidx.test.core.app.ApplicationProvider
import br.com.joaoduartemariucio.rodaro.data.preferences.LocaleLanguageApplier
import br.com.joaoduartemariucio.rodaro.data.preferences.PreferenceGate
import br.com.joaoduartemariucio.rodaro.data.preferences.SharedPreferencesPreferencesRepository
import br.com.joaoduartemariucio.rodaro.domain.preferences.entities.CurrencyOption
import br.com.joaoduartemariucio.rodaro.domain.preferences.entities.LanguageOption
import br.com.joaoduartemariucio.rodaro.domain.preferences.entities.Preferences
import br.com.joaoduartemariucio.rodaro.domain.preferences.entities.UnitSystem
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class PreferenceStoreTests {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val repository = SharedPreferencesPreferencesRepository(context)

    @Test
    fun `returns the defaults when nothing was ever saved`() =
        runTest {
            assertEquals(Preferences(), repository.get())
        }

    @Test
    fun `round trips every preference through shared preferences`() =
        runTest {
            val stored =
                Preferences(
                    pushEnabled = false,
                    remindersEnabled = false,
                    currency = CurrencyOption.UYU,
                    unitSystem = UnitSystem.IMPERIAL,
                    language = LanguageOption.ENGLISH,
                )

            repository.save(stored)

            assertEquals(stored, SharedPreferencesPreferencesRepository(context).get())
        }

    @Test
    fun `ignores values that no longer match an option`() =
        runTest {
            context
                .getSharedPreferences(SharedPreferencesPreferencesRepository.FILE_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(SharedPreferencesPreferencesRepository.KEY_CURRENCY, "XXX")
                .putString(SharedPreferencesPreferencesRepository.KEY_UNITS, "OLD_SYSTEM")
                .putString(SharedPreferencesPreferencesRepository.KEY_LANGUAGE, "OLD_LANGUAGE")
                .commit()

            val loaded = repository.get()

            assertEquals(null, loaded.currency)
            assertEquals(UnitSystem.METRIC, loaded.unitSystem)
            assertEquals(LanguageOption.SYSTEM, loaded.language)
        }

    @Test
    fun `lets system services read the same preferences through the gate`() =
        runTest {
            repository.save(Preferences(pushEnabled = false, remindersEnabled = false))

            val read = PreferenceGate.preferences(context)

            assertEquals(false, read.pushEnabled)
            assertEquals(false, read.remindersEnabled)
        }

    @Test
    fun `applies an explicit language through the platform locale manager`() {
        LocaleLanguageApplier(context).apply(LanguageOption.ENGLISH)

        val locales = context.getSystemService(android.app.LocaleManager::class.java).applicationLocales

        assertEquals(1, locales.size())
        assertEquals("en", locales[0].toLanguageTag())
    }

    @Test
    fun `falls back to the system locales when the language follows the phone`() {
        LocaleLanguageApplier(context).apply(LanguageOption.ENGLISH)

        LocaleLanguageApplier(context).apply(LanguageOption.SYSTEM)

        val locales = context.getSystemService(android.app.LocaleManager::class.java).applicationLocales

        assertEquals(LocaleList.getEmptyLocaleList(), locales)
    }
}
