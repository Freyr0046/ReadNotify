package com.freyr.readmynotify.data.repository

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.freyr.readmynotify.R
import com.freyr.readmynotify.common.SelfTestNotification
import com.freyr.readmynotify.domain.repository.TestNotificationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class TestNotificationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : TestNotificationRepository {

    override suspend fun postTestNotification(): Result<Unit> = runCatching {
        ensureChannelExists()

        val notification = NotificationCompat.Builder(context, SelfTestNotification.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("ReadNotify 測試")
            .setContentText("這是一則測試通知，用來確認播報功能是否正常運作")
            .setCategory(Notification.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(
            SelfTestNotification.TAG,
            SelfTestNotification.NOTIFICATION_ID,
            notification,
        )
    }

    private fun ensureChannelExists() {
        val channel = NotificationChannel(
            SelfTestNotification.CHANNEL_ID,
            "自我測試通知",
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}
