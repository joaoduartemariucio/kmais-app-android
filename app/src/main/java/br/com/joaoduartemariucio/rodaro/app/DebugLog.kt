package br.com.joaoduartemariucio.rodaro.app

import android.util.Log
import br.com.joaoduartemariucio.rodaro.BuildConfig
import br.com.joaoduartemariucio.rodaro.data.common.services.LoggingAnalytics
import br.com.joaoduartemariucio.rodaro.domain.common.services.Analytics

object DebugLog {
    const val HTTP_TAG = "Rodaro.Http"
    const val ANALYTICS_TAG = "Rodaro.Analytics"

    val http: ((String) -> Unit)? =
        if (BuildConfig.DEBUG) {
            { message -> Log.d(HTTP_TAG, message) }
        } else {
            null
        }

    fun analytics(tracker: Analytics): Analytics =
        if (BuildConfig.DEBUG) LoggingAnalytics(tracker) { message -> Log.d(ANALYTICS_TAG, message) } else tracker
}
