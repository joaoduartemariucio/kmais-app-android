package br.com.joaoduartemariucio.rodaro.data.preferences

import android.content.Context
import br.com.joaoduartemariucio.rodaro.domain.preferences.entities.Preferences
import kotlinx.coroutines.runBlocking

object PreferenceGate {
    fun preferences(context: Context): Preferences = runBlocking { SharedPreferencesPreferencesRepository(context).get() }
}
