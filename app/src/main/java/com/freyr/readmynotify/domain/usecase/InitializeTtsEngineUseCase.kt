package com.freyr.readmynotify.domain.usecase

// LOCKED — do not modify during Phase 5
// 新增於 Contract Lock 階段（doc/phase3-contract-lock.md）：
// MainViewModel 於 INIT_CHECKING 與 OnRetryEngineInitClicked 皆需要觸發
// TTS 引擎初始化，若直接依賴 TtsEngineRepository 會違反
// UI → ViewModel → UseCase → Repository 的分層規則，故補上此 UseCase。
interface InitializeTtsEngineUseCase {
    suspend operator fun invoke(): Result<Unit>
}
