package br.com.joaoduartemariucio.rodaro.viewmodels

import br.com.joaoduartemariucio.rodaro.domain.preferences.entities.CurrencyOption
import br.com.joaoduartemariucio.rodaro.domain.preferences.entities.LanguageOption
import br.com.joaoduartemariucio.rodaro.domain.preferences.entities.Preferences
import br.com.joaoduartemariucio.rodaro.domain.preferences.entities.UnitSystem
import br.com.joaoduartemariucio.rodaro.fakes.InMemoryPreferencesRepository
import br.com.joaoduartemariucio.rodaro.presentation.common.DisplayPreferences
import br.com.joaoduartemariucio.rodaro.presentation.common.FormState
import br.com.joaoduartemariucio.rodaro.presentation.preferences.PreferencesViewModel
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PreferencesViewModelTests {
    private var appliedLanguage: LanguageOption? = null
    private var reminderSyncs = 0

    @After
    fun tearDown() {
        DisplayPreferences.update(Preferences())
        appliedLanguage = null
        reminderSyncs = 0
    }

    private fun makeViewModel(repository: InMemoryPreferencesRepository) =
        PreferencesViewModel(
            repository = repository,
            applyLanguage = { appliedLanguage = it },
            syncReminders = { reminderSyncs += 1 },
        )

    @Test
    fun `loads saved preferences into the display holder`() =
        runTest {
            val repository = InMemoryPreferencesRepository(stored = Preferences(currency = CurrencyOption.USD))
            val viewModel = makeViewModel(repository)

            viewModel.load()

            assertTrue(viewModel.loaded)
            assertEquals(CurrencyOption.USD, viewModel.preferences.currency)
            assertEquals(CurrencyOption.USD, DisplayPreferences.current.currency)
            assertIs<FormState.Editing>(viewModel.state)
        }

    @Test
    fun `reports a failure when the stored preferences cannot be read`() =
        runTest {
            val viewModel = makeViewModel(InMemoryPreferencesRepository(failure = RuntimeException("no store")))

            viewModel.load()

            assertFalse(viewModel.loaded)
            assertIs<FormState.Failed>(viewModel.state)
        }

    @Test
    fun `toggling push only persists the new value`() =
        runTest {
            val repository = InMemoryPreferencesRepository()
            val viewModel = makeViewModel(repository)
            viewModel.load()

            viewModel.setPushEnabled(false)

            assertFalse(repository.stored.pushEnabled)
            assertIs<FormState.Saved>(viewModel.state)
            assertEquals(0, reminderSyncs)
            assertNull(appliedLanguage)
        }

    @Test
    fun `turning reminders off cancels the scheduled work through the sync`() =
        runTest {
            val repository = InMemoryPreferencesRepository()
            val viewModel = makeViewModel(repository)
            viewModel.load()

            viewModel.setRemindersEnabled(false)

            assertFalse(repository.stored.remindersEnabled)
            assertEquals(1, reminderSyncs)
        }

    @Test
    fun `picking a currency persists it and updates the display`() =
        runTest {
            val repository = InMemoryPreferencesRepository()
            val viewModel = makeViewModel(repository)
            viewModel.load()

            viewModel.setCurrency(CurrencyOption.EUR)

            assertEquals(CurrencyOption.EUR, repository.stored.currency)
            assertEquals(CurrencyOption.EUR, DisplayPreferences.current.currency)
        }

    @Test
    fun `switching to imperial persists the unit system`() =
        runTest {
            val repository = InMemoryPreferencesRepository()
            val viewModel = makeViewModel(repository)
            viewModel.load()

            viewModel.setUnitSystem(UnitSystem.IMPERIAL)

            assertEquals(UnitSystem.IMPERIAL, repository.stored.unitSystem)
            assertEquals(UnitSystem.IMPERIAL, DisplayPreferences.current.unitSystem)
        }

    @Test
    fun `changing the language applies it after saving`() =
        runTest {
            val repository = InMemoryPreferencesRepository()
            val viewModel = makeViewModel(repository)
            viewModel.load()

            viewModel.setLanguage(LanguageOption.ENGLISH)

            assertEquals(LanguageOption.ENGLISH, repository.stored.language)
            assertEquals(LanguageOption.ENGLISH, appliedLanguage)
        }

    @Test
    fun `keeps the edited value on screen even when saving fails`() =
        runTest {
            val repository = InMemoryPreferencesRepository(failure = RuntimeException("no store"))
            val viewModel = makeViewModel(repository)
            viewModel.load()

            viewModel.setCurrency(CurrencyOption.GBP)

            assertEquals(CurrencyOption.GBP, viewModel.preferences.currency)
            assertIs<FormState.Failed>(viewModel.state)
        }
}
