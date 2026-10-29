package br.com.joaoduartemariucio.rodaro.app

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import br.com.joaoduartemariucio.rodaro.app.navigation.AppNavHost
import br.com.joaoduartemariucio.rodaro.presentation.common.ads.AppOpenAds
import br.com.joaoduartemariucio.rodaro.presentation.common.ads.CurrentActivity
import br.com.joaoduartemariucio.rodaro.presentation.theme.RodaroTheme
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

        val dependencies = (application as RodaroApplication).dependencies
        dependencies.notificationPermission.requester = ::requestNotificationPermission

        setContent {
            RodaroTheme {
                CompositionLocalProvider(LocalDependencies provides dependencies) {
                    AppNavHost()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        CurrentActivity.onResumed(this)
        AppOpenAds.onActivityResumed(this)
    }

    override fun onPause() {
        AppOpenAds.onActivityPaused()
        CurrentActivity.onPaused(this)
        super.onPause()
    }

    override fun onDestroy() {
        val dependencies = (application as RodaroApplication).dependencies
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
