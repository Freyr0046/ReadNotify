package com.freyr.readmynotify.domain.repository

import com.freyr.readmynotify.domain.model.TtsEngineState
import kotlinx.coroutines.flow.StateFlow

// LOCKED — do not modify during Phase 5
interface TtsEngineRepository {
    val engineState: StateFlow<TtsEngineState>

    suspend fun initialize(): Result<Unit>

    /**
     * 語系不支援時回傳 Result.failure；呼叫端必須靜音跳過，
     * 絕不可將錯誤 externalize 為 crash 或亂碼朗讀。
     */
    suspend fun speak(
        text: String,
        utteranceId: String,
    ): Result<Unit>
}
