package br.com.joaoduartemariucio.rodaro.data.preferences

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import br.com.joaoduartemariucio.rodaro.domain.preferences.entities.LanguageOption

class LocaleLanguageApplier(
    private val context: Context,
) {
    fun apply(language: LanguageOption) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val locales =
                if (language ==
                    LanguageOption.SYSTEM
                ) {
                    LocaleList.getEmptyLocaleList()
                } else {
                    LocaleList.forLanguageTags(language.tag)
                }
            context.getSystemService(LocaleManager::class.java).applicationLocales = locales
        } else {
            val locales =
                if (language ==
                    LanguageOption.SYSTEM
                ) {
                    LocaleListCompat.getEmptyLocaleList()
                } else {
                    LocaleListCompat.forLanguageTags(language.tag)
                }
            AppCompatDelegate.setApplicationLocales(locales)
        }
    }
}
