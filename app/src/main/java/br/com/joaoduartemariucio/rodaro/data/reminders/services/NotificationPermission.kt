package br.com.joaoduartemariucio.rodaro.data.reminders.services

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class NotificationPermission(
    private val context: Context,
) {
    var requester: (suspend () -> Boolean)? = null

    val isGranted: Boolean
        get() = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    suspend fun request(): Boolean = isGranted || requester?.invoke() == true
}
