package br.com.joaoduartemariucio.rodaro.data

import android.Manifest
import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import androidx.work.workDataOf
import br.com.joaoduartemariucio.rodaro.data.common.services.SystemDateProvider
import br.com.joaoduartemariucio.rodaro.data.reminders.services.NotificationPermission
import br.com.joaoduartemariucio.rodaro.data.reminders.services.ReminderWorker
import br.com.joaoduartemariucio.rodaro.data.reminders.services.WorkManagerReminderScheduler
import br.com.joaoduartemariucio.rodaro.domain.reminders.services.Reminder
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class WorkManagerReminderSchedulerTests {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val now = Instant.parse("2026-01-01T12:00:00Z")
    private lateinit var permission: NotificationPermission
    private lateinit var scheduler: WorkManagerReminderScheduler

    @Before
    fun setUp() {
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder().setExecutor(SynchronousExecutor()).build(),
        )
        permission = NotificationPermission(context)
        scheduler = WorkManagerReminderScheduler(context, permission) { now }
    }

    private fun reminder(id: UUID = UUID.randomUUID()) = Reminder(id, "Gol: Oil change", "Due on January 2, 2026.", now.plusSeconds(86_400))

    @Test
    fun `scheduled reminders are pending until cancelled`() =
        runTest {
            val first = reminder()
            val second = reminder()

            scheduler.schedule(first)
            scheduler.schedule(second)
            scheduler.schedule(first.copy(body = "updated"))

            assertEquals(setOf(first.id, second.id), scheduler.pendingIdentifiers().toSet())

            scheduler.cancel(first.id)
            assertEquals(listOf(second.id), scheduler.pendingIdentifiers())
        }

    @Test
    fun `authorization uses the permission, then the requester`() =
        runTest {
            assertFalse(scheduler.requestAuthorization())

            permission.requester = { true }
            assertTrue(scheduler.requestAuthorization())

            permission.requester = null
            shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
            assertTrue(scheduler.requestAuthorization())
        }

    @Test
    fun `the worker posts the notification when allowed`() =
        runTest {
            val input = workDataOf(ReminderWorker.KEY_ID to UUID.randomUUID().toString(), ReminderWorker.KEY_TITLE to "Gol", ReminderWorker.KEY_BODY to "Due")
            val worker = TestListenableWorkerBuilder<ReminderWorker>(context).setInputData(input).build()

            assertEquals(ListenableWorker.Result.success(), worker.doWork())

            shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
            assertEquals(ListenableWorker.Result.success(), worker.doWork())
            val manager = context.getSystemService(android.app.NotificationManager::class.java)
            assertEquals(1, shadowOf(manager).allNotifications.size)

            val missingId = TestListenableWorkerBuilder<ReminderWorker>(context).build()
            assertEquals(ListenableWorker.Result.failure(), missingId.doWork())
        }

    @Test
    fun `the system clock reports the current zone`() {
        val provider = SystemDateProvider()

        assertEquals(ZoneId.systemDefault(), provider.zone)
        assertTrue(provider.now >= now)
        WorkManager.getInstance(context)
    }
}
