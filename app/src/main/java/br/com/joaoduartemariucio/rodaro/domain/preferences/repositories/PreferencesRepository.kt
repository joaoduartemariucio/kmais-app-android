package br.com.joaoduartemariucio.rodaro.domain.preferences.repositories

import br.com.joaoduartemariucio.rodaro.domain.preferences.entities.Preferences

interface PreferencesRepository {
    suspend fun get(): Preferences

    suspend fun save(preferences: Preferences)
}
