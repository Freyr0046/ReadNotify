package com.freyr.readmynotify

import android.app.Notification
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.freyr.readmynotify.common.SelfTestNotification
import com.freyr.readmynotify.domain.model.IncomingNotification
import com.freyr.readmynotify.domain.usecase.BuildAnnouncementUseCase
import com.freyr.readmynotify.domain.usecase.EnqueueAnnouncementUseCase
import com.freyr.readmynotify.domain.usecase.ObserveWhitelistUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 精簡為轉接層：空值檢查 → 自我黑名單（測試標記例外）→ 白名單過濾 →
 * 交給 Domain 層組裝並播報。業務邏輯（模板/截斷/URL 取代/佇列）皆已下放到
 * BuildAnnouncementUseCase 與 EnqueueAnnouncementUseCase（doc/phase1-spec.md
 * Layer Map）。
 */
@AndroidEntryPoint
class NotificationService : NotificationListenerService() {
    @Inject
    lateinit var buildAnnouncementUseCase: BuildAnnouncementUseCase

    @Inject
    lateinit var enqueueAnnouncementUseCase: EnqueueAnnouncementUseCase

    @Inject
    lateinit var observeWhitelistUseCase: ObserveWhitelistUseCase

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var whitelist: Set<String> = emptySet()

    override fun onCreate() {
        super.onCreate()
        serviceScope.launch {
            observeWhitelistUseCase().collect { whitelist = it }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val isSelfTestNotification = sbn.tag == SelfTestNotification.TAG

        // 3.1.1 自我無限循環防護：本 App 自身通知一律阻擋，唯有自我測試工具
        // 發出的特殊標記通知為唯一例外（doc/phase3-contract-lock.md 決議 #4）。
        if (packageName == applicationContext.packageName && !isSelfTestNotification) {
            return
        }

        // 白名單過濾：預設全域關閉，僅使用者勾選的 App 會被朗讀；測試通知
        // 不受白名單限制，永遠允許通過以驗證播報功能。
        if (!isSelfTestNotification && packageName !in whitelist) {
            return
        }

        val extras: Bundle = sbn.notification?.extras ?: return
        val incomingNotification =
            IncomingNotification(
                packageName = packageName,
                title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
                content = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
                groupName = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString(),
                category = sbn.notification?.category,
                tag = sbn.tag,
            )

        val appLabel = resolveAppLabel(packageName)
        val announcement = buildAnnouncementUseCase(appLabel, incomingNotification) ?: return

        serviceScope.launch {
            enqueueAnnouncementUseCase(announcement)
        }
    }

    private fun resolveAppLabel(packageName: String): String =
        runCatching {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        }.getOrDefault(packageName)

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
