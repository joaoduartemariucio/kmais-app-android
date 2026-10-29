package br.com.joaoduartemariucio.rodaro.data.preferences

import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList
import br.com.joaoduartemariucio.rodaro.domain.preferences.entities.LanguageOption

class LocaleLanguageApplier(
    private val context: Context,
) {
    fun apply(language: LanguageOption) {
        val manager = context.getSystemService(LocaleManager::class.java)
        if (language == LanguageOption.SYSTEM) {
            manager.setApplicationLocales(LocaleList.getEmptyLocaleList())
        } else {
            manager.setApplicationLocales(LocaleList.forLanguageTags(language.tag))
        }
    }
}
