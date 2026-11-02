package br.com.joaoduartemariucio.rodaro.presentation.common.ads

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object AdMobGate {
    @Volatile
    private var enabled = true

    var enabledState: Boolean by mutableStateOf(true)
        private set

    fun isEnabled(): Boolean = enabled

    fun update(value: Boolean) {
        enabled = value
        enabledState = value
    }
}
