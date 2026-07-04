package com.freyr.readmynotify.domain.usecase

import com.freyr.readmynotify.domain.model.TtsEngineState
import kotlinx.coroutines.flow.StateFlow

// LOCKED — do not modify during Phase 5
// 新增於 Contract Lock 階段，理由同 InitializeTtsEngineUseCase。
interface ObserveEngineStateUseCase {
    operator fun invoke(): StateFlow<TtsEngineState>
}
