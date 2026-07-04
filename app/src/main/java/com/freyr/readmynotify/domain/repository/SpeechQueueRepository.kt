package com.freyr.readmynotify.domain.repository

import com.freyr.readmynotify.domain.model.Announcement
import com.freyr.readmynotify.domain.model.PlaybackState
import kotlinx.coroutines.flow.StateFlow

// LOCKED — do not modify during Phase 5
interface SpeechQueueRepository {
    val playbackState: StateFlow<PlaybackState>

    /**
     * 佇列已積壓 5 條時，本次呼叫靜默捨棄新通知並仍回傳 Result.success（
     * 依 PRD 異常矩陣，捨棄不是錯誤，呼叫端不應因此中斷）。
     */
    suspend fun enqueue(announcement: Announcement): Result<Unit>
}
