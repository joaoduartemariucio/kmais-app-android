package br.com.joaoduartemariucio.kmais_app_android.app

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import br.com.joaoduartemariucio.kmais_app_android.presentation.theme.KmaisTheme
import kotlinx.coroutines.CompletableDeferred

class MainActivity : ComponentActivity() {
    private var pendingPermission: CompletableDeferred<Boolean>? = null

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            pendingPermission?.complete(granted)
            pendingPermission = null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val dependencies = (application as KmaisApplication).dependencies
        dependencies.notificationPermission.requester = ::requestNotificationPermission

        setContent {
            KmaisTheme {
                CompositionLocalProvider(LocalDependencies provides dependencies) {
                    RootView()
                }
            }
        }
    }

    override fun onDestroy() {
        val dependencies = (application as KmaisApplication).dependencies
        if (dependencies.notificationPermission.requester == ::requestNotificationPermission) {
            dependencies.notificationPermission.requester = null
        }
        super.onDestroy()
    }

    private suspend fun requestNotificationPermission(): Boolean {
        val deferred =
            pendingPermission ?: CompletableDeferred<Boolean>().also {
                pendingPermission = it
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        return deferred.await()
    }
}
