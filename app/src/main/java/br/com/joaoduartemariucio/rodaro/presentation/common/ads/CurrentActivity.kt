package br.com.joaoduartemariucio.rodaro.presentation.common.ads

import android.app.Activity
import java.lang.ref.WeakReference

object CurrentActivity {
    fun onResumed(activity: Activity) {
        reference = WeakReference(activity)
    }

    fun onPaused(activity: Activity) {
        if (reference.get() === activity) reference = WeakReference(null)
    }

    val activity: Activity?
        get() = reference.get()

    private var reference = WeakReference<Activity>(null)
}
