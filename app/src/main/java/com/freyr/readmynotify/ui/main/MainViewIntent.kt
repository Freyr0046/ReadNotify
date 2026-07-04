package com.freyr.readmynotify.ui.main

// LOCKED — do not modify during Phase 5
sealed interface MainViewIntent {

    /** 前往系統通知存取權限設定頁 */
    data object OnPermissionSettingsClicked : MainViewIntent

    /**
     * 畫面回到前景時觸發（DisposableEffect + Lifecycle.Event.ON_RESUME），
     * 對應異常矩陣「使用者中途關閉通知權限」防禦規則。
     */
    data object OnScreenResumed : MainViewIntent

    data class OnAppWhitelistToggled(val packageName: String, val checked: Boolean) : MainViewIntent

    /** 按下「發送測試通知」，2 秒後觸發模擬通知 */
    data object OnSendTestNotificationClicked : MainViewIntent

    /** ENGINE_ERROR 狀態下，若原因為 TTS_ENGINE_NOT_INSTALLED，導向 Play 商店下載頁 */
    data object OnInstallTtsEngineClicked : MainViewIntent

    /** 使用者切換至系統設定修復 TTS 語系後，手動重試初始化 */
    data object OnRetryEngineInitClicked : MainViewIntent
}
