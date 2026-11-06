package br.com.joaoduartemariucio.rodaro.data

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.test.core.app.ApplicationProvider
import br.com.joaoduartemariucio.rodaro.data.preferences.LocaleLanguageApplier
import br.com.joaoduartemariucio.rodaro.domain.preferences.entities.LanguageOption
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class LocaleLanguageApplierTests {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @After
    fun tearDown() {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
    }

    @Test
    fun `before Android 13 an explicit language goes through AppCompat`() {
        LocaleLanguageApplier(context).apply(LanguageOption.ENGLISH)

        val locales = AppCompatDelegate.getApplicationLocales()

        assertEquals(1, locales.size())
        assertEquals("en", locales[0]?.toLanguageTag())
    }

    @Test
    fun `before Android 13 following the phone clears the AppCompat override`() {
        LocaleLanguageApplier(context).apply(LanguageOption.ENGLISH)

        LocaleLanguageApplier(context).apply(LanguageOption.SYSTEM)

        assertTrue(AppCompatDelegate.getApplicationLocales().isEmpty)
    }
}
