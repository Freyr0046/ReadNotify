package com.freyr.readmynotify.domain.usecase

import com.freyr.readmynotify.domain.model.PlaybackState
import kotlinx.coroutines.flow.StateFlow

// LOCKED — do not modify during Phase 5
// 新增於 Contract Lock 階段，理由同 InitializeTtsEngineUseCase：
// MainViewModel 需要觀察播放狀態以切換 TtsPlaying/IdleConfig，
// 不可直接依賴 SpeechQueueRepository。
interface ObservePlaybackStateUseCase {
    operator fun invoke(): StateFlow<PlaybackState>
}
