package br.com.joaoduartemariucio.rodaro.presentation.profile.navigation

import androidx.navigation3.runtime.EntryProviderScope
import br.com.joaoduartemariucio.rodaro.presentation.common.navigation.Route
import br.com.joaoduartemariucio.rodaro.presentation.profile.ProfileScreen

fun EntryProviderScope<Route>.profileEntries() {
    entry<ProfileRoute.Profile> { ProfileScreen() }
}
