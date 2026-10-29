package br.com.joaoduartemariucio.rodaro.presentation.common.ads

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.isVisible
import br.com.joaoduartemariucio.rodaro.R
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

@SuppressLint("InflateParams")
@Composable
fun AdNative(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val unitId = stringResource(R.string.admob_native_unit_id)
    var ad by remember { mutableStateOf<NativeAd?>(null) }
    val loader =
        remember(context, unitId) {
            val listener =
                object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        ad = null
                    }
                }
            val builder = AdLoader.Builder(context, unitId)
            builder.forNativeAd { newAd ->
                val previous = ad
                previous?.destroy()
                ad = newAd
            }
            builder.withAdListener(listener)
            builder.build()
        }

    LaunchedEffect(loader) { loader.loadAd(AdRequest.Builder().build()) }

    DisposableEffect(Unit) {
        onDispose { ad?.destroy() }
    }

    val currentAd = ad ?: return

    Surface(
        modifier = modifier.padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { viewContext -> LayoutInflater.from(viewContext).inflate(R.layout.ad_native, null) as NativeAdView },
            update = { nativeAdView -> bindNativeAd(nativeAdView, currentAd) },
        )
    }
}

private fun bindNativeAd(
    nativeAdView: NativeAdView,
    ad: NativeAd,
) {
    val headline = nativeAdView.findViewById<TextView>(R.id.ad_headline)
    val body = nativeAdView.findViewById<TextView>(R.id.ad_body)
    val advertiser = nativeAdView.findViewById<TextView>(R.id.ad_advertiser)
    val callToAction = nativeAdView.findViewById<TextView>(R.id.ad_call_to_action)
    val icon = nativeAdView.findViewById<ImageView>(R.id.ad_icon)
    val media = nativeAdView.findViewById<MediaView>(R.id.ad_media)

    headline.bindText(ad.headline)
    body.bindText(ad.body)
    advertiser.bindText(ad.advertiser)
    callToAction.bindText(ad.callToAction)

    val adIcon = ad.icon
    if (adIcon == null) {
        icon.isVisible = false
    } else {
        icon.isVisible = true
        icon.setImageDrawable(adIcon.drawable)
    }

    val mediaContent = ad.mediaContent
    if (mediaContent == null) {
        media.isVisible = false
    } else {
        media.isVisible = true
        media.setMediaContent(mediaContent)
    }

    nativeAdView.headlineView = headline
    nativeAdView.bodyView = body
    nativeAdView.advertiserView = advertiser
    nativeAdView.callToActionView = callToAction
    nativeAdView.iconView = icon
    nativeAdView.mediaView = media
    nativeAdView.setNativeAd(ad)
}

private fun TextView.bindText(value: String?) {
    if (value.isNullOrEmpty()) {
        isVisible = false
    } else {
        isVisible = true
        text = value
    }
}
