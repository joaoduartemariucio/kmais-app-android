package br.com.joaoduartemariucio.rodaro.data.common.services

import android.content.Context
import androidx.core.os.bundleOf
import br.com.joaoduartemariucio.rodaro.domain.common.services.Analytics
import com.google.firebase.analytics.FirebaseAnalytics

class FirebaseAnalyticsTracker(
    context: Context,
) : Analytics {
    private val analytics = FirebaseAnalytics.getInstance(context)

    override fun screen(name: String) {
        analytics.logEvent(
            FirebaseAnalytics.Event.SCREEN_VIEW,
            bundleOf(
                FirebaseAnalytics.Param.SCREEN_NAME to name,
                FirebaseAnalytics.Param.SCREEN_CLASS to name,
            ),
        )
    }

    override fun event(
        name: String,
        parameters: Map<String, String>,
    ) {
        analytics.logEvent(name, bundleOf(*parameters.map { (key, value) -> key to value }.toTypedArray()))
    }
}
