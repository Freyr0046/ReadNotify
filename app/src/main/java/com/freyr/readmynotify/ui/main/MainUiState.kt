package com.freyr.readmynotify.ui.main

import com.freyr.readmynotify.domain.model.EngineErrorReason

// LOCKED — do not modify during Phase 5
sealed interface MainUiState {
    /** App 啟動時，檢查通知存取權限與 TTS 引擎可用性 */
    data object InitChecking : MainUiState

    /** 無通知監聽權限：全螢幕阻擋，僅顯示前往設定按鈕 */
    data object PermissionDenied : MainUiState

    /** 權限正常且目前無語音播放中：顯示白名單清單、字數限制、測試按鈕 */
    data class IdleConfig(
        val installedApps: List<AppWhitelistItem>,
        val maxCharLimit: Int = 50,
    ) : MainUiState

    /** 背景正在播報 */
    data class TtsPlaying(
        val speakingFromLabel: String,
        val installedApps: List<AppWhitelistItem>,
    ) : MainUiState

    /** TTS 初始化失敗或遺失語音元件 */
    data class EngineError(val reason: EngineErrorReason) : MainUiState
}

/**
 * 精簡、穩定、可比較的資料模型，避免不必要的 recomposition。
 * App 圖示不放進 UiState（Drawable 不可比較/不穩定），由 UI 層依 packageName
 * 自行以 remember{} 快取載入。
 */
data class AppWhitelistItem(
    val packageName: String,
    val appLabel: String,
    val isChecked: Boolean,
)
