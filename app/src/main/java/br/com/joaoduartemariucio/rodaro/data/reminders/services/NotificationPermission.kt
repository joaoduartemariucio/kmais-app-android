package br.com.joaoduartemariucio.rodaro.data.reminders.services

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class NotificationPermission(
    private val context: Context,
) {
    var requester: (suspend () -> Boolean)? = null

    val isGranted: Boolean
        get() =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                NotificationManagerCompat.from(context).areNotificationsEnabled()
            }

    suspend fun request(): Boolean = isGranted || (isRuntimePermission && requester?.invoke() == true)

    private val isRuntimePermission: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
}
