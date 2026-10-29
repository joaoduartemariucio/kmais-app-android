package br.com.joaoduartemariucio.rodaro.app

import android.app.Application
import android.content.pm.ApplicationInfo
import br.com.joaoduartemariucio.rodaro.R
import br.com.joaoduartemariucio.rodaro.data.push.services.PushMessagingService
import br.com.joaoduartemariucio.rodaro.presentation.common.ads.AppOpenAds
import br.com.joaoduartemariucio.rodaro.presentation.common.ads.InterstitialAds
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object FirebaseSetup {
    fun start(application: Application) {
        if (FirebaseApp.getApps(application).isEmpty() && FirebaseApp.initializeApp(application) == null) return

        val isDebuggable = application.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!isDebuggable)

        FirebaseRemoteConfig.getInstance().apply {
            setDefaultsAsync(R.xml.remote_config_defaults)
            fetchAndActivate()
        }

        PushMessagingService.createChannel(application)

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            MobileAds.initialize(application)
            withContext(Dispatchers.Main) {
                InterstitialAds.prepare(application)
                AppOpenAds.start(application)
            }
        }
    }
}
