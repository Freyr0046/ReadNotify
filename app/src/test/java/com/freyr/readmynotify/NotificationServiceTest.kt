package com.freyr.readmynotify

import android.app.Notification
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 修正「LINE 訊息被唸兩次」的迴歸測試：LINE 等聊天 App 常會為同一則訊息
 * 額外發出一則帶 FLAG_GROUP_SUMMARY 的群組摘要通知，必須被過濾掉。
 */
class NotificationServiceTest {
    @Test
    fun `notification with FLAG_GROUP_SUMMARY is treated as a group summary`() {
        val notification = Notification().apply { flags = Notification.FLAG_GROUP_SUMMARY }

        assertTrue(isGroupSummaryNotification(notification))
    }

    @Test
    fun `ordinary notification without FLAG_GROUP_SUMMARY is not a group summary`() {
        val notification = Notification().apply { flags = 0 }

        assertFalse(isGroupSummaryNotification(notification))
    }

    @Test
    fun `notification with other flags set is not mistaken for a group summary`() {
        val notification =
            Notification().apply {
                flags = Notification.FLAG_AUTO_CANCEL or Notification.FLAG_ONGOING_EVENT
            }

        assertFalse(isGroupSummaryNotification(notification))
    }

    @Test
    fun `null notification is not treated as a group summary`() {
        assertFalse(isGroupSummaryNotification(null))
    }
}
