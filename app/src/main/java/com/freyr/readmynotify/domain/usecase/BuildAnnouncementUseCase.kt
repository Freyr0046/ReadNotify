package com.freyr.readmynotify.domain.usecase

import com.freyr.readmynotify.domain.model.Announcement
import com.freyr.readmynotify.domain.model.IncomingNotification

// LOCKED — do not modify during Phase 5
interface BuildAnnouncementUseCase {
    /**
     * 回傳 null 代表依 PRD 防禦規則應「靜音跳過」（標題/內文為空等情況），
     * 呼叫端收到 null 時不得呼叫 EnqueueAnnouncementUseCase。
     */
    operator fun invoke(
        appLabel: String,
        notification: IncomingNotification,
    ): Announcement?
}
