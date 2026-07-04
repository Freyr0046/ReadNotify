package com.freyr.readmynotify.data.repository

import android.content.Context
import android.provider.Settings
import com.freyr.readmynotify.domain.repository.NotificationAccessRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class NotificationAccessRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : NotificationAccessRepository {

    override fun isNotificationAccessGranted(): Boolean {
        val packageName = context.packageName
        val flat = Settings.Secure.getString(context.contentResolver, ENABLED_NOTIFICATION_LISTENERS)
        if (flat.isNullOrEmpty()) return false

        // 格式為 "package/component.Class"（ComponentName.flattenToString）以 ':' 分隔多筆，
        // 用純字串解析避免依賴 android.content.ComponentName（單元測試無法對其 stub）。
        return flat.split(":").any { entry -> entry.substringBefore('/') == packageName }
    }

    private companion object {
        const val ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners"
    }
}
