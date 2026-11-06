package br.com.joaoduartemariucio.rodaro.presentation.common.ads

import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdRequest

object AdMobGate {
    @Volatile
    private var enabled = false

    @Volatile
    private var personalized = false

    var enabledState: Boolean by mutableStateOf(false)
        private set

    fun isEnabled(): Boolean = enabled

    fun isPersonalized(): Boolean = personalized

    fun update(value: Boolean) {
        enabled = value
        enabledState = value
    }

    fun updatePersonalized(value: Boolean) {
        personalized = value
    }

    fun request(): AdRequest {
        val builder = AdRequest.Builder()
        if (!personalized) {
            builder.addNetworkExtrasBundle(AdMobAdapter::class.java, Bundle().apply { putString("npa", "1") })
        }
        return builder.build()
    }
}
