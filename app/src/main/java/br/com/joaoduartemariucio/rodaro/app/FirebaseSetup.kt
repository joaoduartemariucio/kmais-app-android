package br.com.joaoduartemariucio.rodaro.app

import android.app.Application
import android.content.pm.ApplicationInfo
import br.com.joaoduartemariucio.rodaro.R
import br.com.joaoduartemariucio.rodaro.data.preferences.PreferenceGate
import br.com.joaoduartemariucio.rodaro.data.push.services.PushMessagingService
import br.com.joaoduartemariucio.rodaro.presentation.common.ads.AdMobGate
import br.com.joaoduartemariucio.rodaro.presentation.common.ads.AppOpenAds
import br.com.joaoduartemariucio.rodaro.presentation.common.ads.InterstitialAds
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object FirebaseSetup {
    private const val ENABLE_AD_MOB = "enableAdMob"

    @Volatile
    private var mobileAdsReady = false

    fun start(application: Application) {
        if (FirebaseApp.getApps(application).isEmpty() && FirebaseApp.initializeApp(application) == null) return

        val isDebuggable = application.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        PrivacySettings.apply(application, PreferenceGate.preferences(application))

        val remoteConfig = FirebaseRemoteConfig.getInstance()
        remoteConfig.setConfigSettingsAsync(
            FirebaseRemoteConfigSettings
                .Builder()
                .setMinimumFetchIntervalInSeconds(if (isDebuggable) 0 else 3600)
                .build(),
        )
        applyAdMobFlag(remoteConfig, application)
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults).addOnCompleteListener {
            applyAdMobFlag(remoteConfig, application)
            remoteConfig.fetchAndActivate().addOnCompleteListener {
                applyAdMobFlag(remoteConfig, application)
            }
        }

        PushMessagingService.createChannel(application)

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            MobileAds.initialize(application)
            mobileAdsReady = true
            withContext(Dispatchers.Main) {
                if (AdMobGate.isEnabled()) {
                    InterstitialAds.prepare(application)
                    AppOpenAds.start(application)
                }
            }
        }
    }

    private fun applyAdMobFlag(
        remoteConfig: FirebaseRemoteConfig,
        application: Application,
    ) {
        val enabled = remoteConfig.getBoolean(ENABLE_AD_MOB)
        AdMobGate.update(enabled)
        if (enabled && mobileAdsReady) {
            InterstitialAds.prepare(application)
            AppOpenAds.start(application)
        }
    }
}
