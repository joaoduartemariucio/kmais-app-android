package br.com.joaoduartemariucio.rodaro.presentation.common.ads

import android.app.Activity
import android.app.Application
import br.com.joaoduartemariucio.rodaro.R
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import java.lang.ref.WeakReference

object AppOpenAds {
    private const val SHOW_INTERVAL_MILLIS = 2 * 60 * 60 * 1000L

    private var application: Application? = null
    private var ad: AppOpenAd? = null
    private var isLoading = false
    private var isShowing = false
    private var isForeground = false
    private var hasShown = false
    private var lastShownAt = 0L
    private var activityReference = WeakReference<Activity>(null)

    fun start(application: Application) {
        this.application = application
        load(application)
    }

    fun onActivityResumed(activity: Activity) {
        isForeground = true
        activityReference = WeakReference(activity)
        showIfPossible()
    }

    fun onActivityPaused() {
        isForeground = false
        activityReference = WeakReference(null)
    }

    private fun load(application: Application) {
        if (ad != null || isLoading) return
        isLoading = true
        AppOpenAd.load(
            application,
            application.getString(R.string.admob_app_open_unit_id),
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(loaded: AppOpenAd) {
                    ad = loaded
                    isLoading = false
                    showIfPossible()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoading = false
                }
            },
        )
    }

    private fun showIfPossible() {
        if (!isForeground || isShowing) return
        val loaded = ad ?: return
        if (hasShown && System.currentTimeMillis() - lastShownAt < SHOW_INTERVAL_MILLIS) return
        val activity = activityReference.get() ?: return

        val callback =
            object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    ad = null
                    isShowing = false
                    hasShown = true
                    lastShownAt = System.currentTimeMillis()
                    prepareNext()
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    ad = null
                    isShowing = false
                    prepareNext()
                }
            }
        isShowing = true
        loaded.fullScreenContentCallback = callback
        loaded.show(activity)
    }

    private fun prepareNext() {
        val app = application ?: return
        load(app)
    }
}
