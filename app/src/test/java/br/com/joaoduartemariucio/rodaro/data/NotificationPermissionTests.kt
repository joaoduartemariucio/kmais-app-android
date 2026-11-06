package br.com.joaoduartemariucio.rodaro.data

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import br.com.joaoduartemariucio.rodaro.data.reminders.services.NotificationPermission
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class NotificationPermissionTests {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val permission = NotificationPermission(context)

    private fun notificationsEnabled(enabled: Boolean) {
        shadowOf(context.getSystemService(NotificationManager::class.java)).setNotificationsEnabled(enabled)
    }

    @Test
    @Config(sdk = [28])
    fun `before Android 13 notifications count as granted while the app's notifications are on`() =
        runTest {
            notificationsEnabled(true)

            assertTrue(permission.isGranted)
            assertTrue(permission.request())
        }

    @Test
    @Config(sdk = [28])
    fun `before Android 13 turned-off notifications are denied without asking`() =
        runTest {
            notificationsEnabled(false)
            var asked = false
            permission.requester = {
                asked = true
                true
            }

            assertFalse(permission.isGranted)
            assertFalse(permission.request())
            assertFalse(asked)
        }

    @Test
    @Config(sdk = [33])
    fun `from Android 13 the runtime permission decides, then the requester`() =
        runTest {
            notificationsEnabled(true)

            assertFalse(permission.isGranted)
            assertFalse(permission.request())

            permission.requester = { true }
            assertTrue(permission.request())

            permission.requester = null
            shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
            assertTrue(permission.isGranted)
        }
}
