package com.freyr.readmynotify.common

/**
 * 3.1.5 自我測試工具用的通知標記。NotificationService 以 [TAG] 判斷「這是
 * 允許放行的自我測試訊號」，作為 3.1.1 自我黑名單的唯一例外（doc/phase3
 * -contract-lock.md 決議 #4）。
 */
object SelfTestNotification {
    const val TAG = "com.freyr.readnotify.SELF_TEST"
    const val NOTIFICATION_ID = 9999
    const val CHANNEL_ID = "self_test_channel"
}
