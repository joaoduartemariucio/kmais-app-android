package br.com.joaoduartemariucio.rodaro.app

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import br.com.joaoduartemariucio.rodaro.domain.preferences.entities.Preferences
import br.com.joaoduartemariucio.rodaro.presentation.common.ads.AdMobGate
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.perf.FirebasePerformance
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

object PrivacySettings {
    private const val TAG = "Rodaro.Privacy"

    fun apply(
        context: Context,
        preferences: Preferences,
    ) {
        AdMobGate.updatePersonalized(preferences.personalizedAdsEnabled)
        if (FirebaseApp.getApps(context).isEmpty()) return
        val isDebuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        val collect = preferences.usageStatisticsEnabled
        val analytics = FirebaseAnalytics.getInstance(context)
        analytics.setAnalyticsCollectionEnabled(collect)
        analytics.setConsent(consent(collect, preferences.personalizedAdsEnabled))
        FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = collect && !isDebuggable
        FirebasePerformance.getInstance().isPerformanceCollectionEnabled = collect
    }

    suspend fun resetIdentifiers(context: Context) {
        AdMobGate.updatePersonalized(false)
        if (FirebaseApp.getApps(context).isEmpty()) return
        FirebaseAnalytics.getInstance(context).resetAnalyticsData()
        FirebaseCrashlytics.getInstance().deleteUnsentReports()
        attempt { FirebaseMessaging.getInstance().deleteToken().await() }
        attempt { FirebaseInstallations.getInstance().delete().await() }
    }

    private fun consent(
        statistics: Boolean,
        personalizedAds: Boolean,
    ): Map<FirebaseAnalytics.ConsentType, FirebaseAnalytics.ConsentStatus> {
        fun status(granted: Boolean) = if (granted) FirebaseAnalytics.ConsentStatus.GRANTED else FirebaseAnalytics.ConsentStatus.DENIED
        return mapOf(
            FirebaseAnalytics.ConsentType.ANALYTICS_STORAGE to status(statistics),
            FirebaseAnalytics.ConsentType.AD_STORAGE to status(personalizedAds),
            FirebaseAnalytics.ConsentType.AD_USER_DATA to status(personalizedAds),
            FirebaseAnalytics.ConsentType.AD_PERSONALIZATION to status(personalizedAds),
        )
    }

    private suspend fun attempt(action: suspend () -> Unit) {
        try {
            action()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.w(TAG, "Could not reset a Firebase identifier", error)
        }
    }
}
