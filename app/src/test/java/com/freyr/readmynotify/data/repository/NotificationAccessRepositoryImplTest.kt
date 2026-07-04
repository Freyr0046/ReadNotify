package com.freyr.readmynotify.data.repository

import android.content.ContentResolver
import android.content.Context
import android.provider.Settings
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NotificationAccessRepositoryImplTest {
    private val contentResolver = mockk<ContentResolver>()
    private val context =
        mockk<Context> {
            every { packageName } returns "com.freyr.readnotify"
            every { contentResolver } returns this@NotificationAccessRepositoryImplTest.contentResolver
        }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Settings.Secure::class)
    }

    private fun stubEnabledListeners(value: String?) {
        mockkStatic(Settings.Secure::class)
        every {
            Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        } returns value
    }

    @Test
    fun `returns true when package listed in enabled_notification_listeners`() {
        stubEnabledListeners(
            "com.other/com.other.Service:com.freyr.readnotify/com.freyr.readnotify.NotificationService",
        )

        val repository = NotificationAccessRepositoryImpl(context)

        assertTrue(repository.isNotificationAccessGranted())
    }

    @Test
    fun `returns false when package not listed`() {
        stubEnabledListeners("com.other/com.other.Service")

        val repository = NotificationAccessRepositoryImpl(context)

        assertFalse(repository.isNotificationAccessGranted())
    }

    @Test
    fun `returns false when settings string is empty`() {
        stubEnabledListeners("")

        val repository = NotificationAccessRepositoryImpl(context)

        assertFalse(repository.isNotificationAccessGranted())
    }

    @Test
    fun `returns false when settings string is null`() {
        stubEnabledListeners(null)

        val repository = NotificationAccessRepositoryImpl(context)

        assertFalse(repository.isNotificationAccessGranted())
    }
}
