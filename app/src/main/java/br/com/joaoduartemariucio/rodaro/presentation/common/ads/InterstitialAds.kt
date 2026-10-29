package br.com.joaoduartemariucio.rodaro.presentation.common.ads

import android.content.Context
import br.com.joaoduartemariucio.rodaro.R
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object InterstitialAds {
    private var ad: InterstitialAd? = null
    private var isLoading = false
    private var isShowing = false

    fun prepare(context: Context) {
        if (ad != null || isLoading) return
        val application = context.applicationContext
        isLoading = true
        InterstitialAd.load(
            application,
            application.getString(R.string.admob_interstitial_unit_id),
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(loaded: InterstitialAd) {
                    ad = loaded
                    isLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoading = false
                }
            },
        )
    }

    fun show() {
        val activity = CurrentActivity.activity ?: return
        val loaded = ad ?: return
        if (isShowing) return

        val callback =
            object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    ad = null
                    isShowing = false
                    prepare(activity.applicationContext)
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    ad = null
                    isShowing = false
                    prepare(activity.applicationContext)
                }
            }
        isShowing = true
        loaded.fullScreenContentCallback = callback
        loaded.show(activity)
    }
}
