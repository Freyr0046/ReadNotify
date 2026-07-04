package com.freyr.readmynotify.ui.main

import com.freyr.readmynotify.domain.usecase.CheckNotificationAccessUseCase
import com.freyr.readmynotify.domain.usecase.GetInstalledAppsUseCase
import com.freyr.readmynotify.domain.usecase.InitializeTtsEngineUseCase
import com.freyr.readmynotify.domain.usecase.ObserveEngineStateUseCase
import com.freyr.readmynotify.domain.usecase.ObservePlaybackStateUseCase
import com.freyr.readmynotify.domain.usecase.ObserveWhitelistUseCase
import com.freyr.readmynotify.domain.usecase.SendTestNotificationUseCase
import com.freyr.readmynotify.domain.usecase.SetAppWhitelistedUseCase
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/**
 * LOCKED — do not modify or delete during Phase 5.
 *
 * Each test maps 1:1 to a Gherkin scenario in doc/phase3-contract-lock.md.
 * Bodies are `TODO()` on purpose — they must FAIL until Phase 5 fills in the
 * real assertions against a fully implemented [MainViewModel]. If a stub
 * seems wrong during implementation, escalate (Phase 6) — do not edit it to
 * match the implementation.
 */
class MainViewModelTest {

    private val checkNotificationAccessUseCase: CheckNotificationAccessUseCase = mockk()
    private val initializeTtsEngineUseCase: InitializeTtsEngineUseCase = mockk()
    private val observeEngineStateUseCase: ObserveEngineStateUseCase = mockk()
    private val observePlaybackStateUseCase: ObservePlaybackStateUseCase = mockk()
    private val getInstalledAppsUseCase: GetInstalledAppsUseCase = mockk()
    private val observeWhitelistUseCase: ObserveWhitelistUseCase = mockk()
    private val setAppWhitelistedUseCase: SetAppWhitelistedUseCase = mockk()
    private val sendTestNotificationUseCase: SendTestNotificationUseCase = mockk()

    private fun createViewModel(): MainViewModel = MainViewModel(
        checkNotificationAccessUseCase = checkNotificationAccessUseCase,
        initializeTtsEngineUseCase = initializeTtsEngineUseCase,
        observeEngineStateUseCase = observeEngineStateUseCase,
        observePlaybackStateUseCase = observePlaybackStateUseCase,
        getInstalledAppsUseCase = getInstalledAppsUseCase,
        observeWhitelistUseCase = observeWhitelistUseCase,
        setAppWhitelistedUseCase = setAppWhitelistedUseCase,
        sendTestNotificationUseCase = sendTestNotificationUseCase,
    )

    // Scenario: App 啟動時進入初始化檢查
    @Test
    fun `initial state is InitChecking`() = runTest {
        TODO("assert createViewModel().uiState.value is MainUiState.InitChecking before any check completes")
    }

    // Scenario: 初始化檢查後偵測到未授權通知存取權限
    @Test
    fun `permission denied transitions to PermissionDenied`() = runTest {
        TODO("mock checkNotificationAccessUseCase() = false, assert uiState becomes PermissionDenied")
    }

    // Scenario: 初始化檢查後偵測到權限已授權且 TTS 就緒
    @Test
    fun `permission granted and tts ready transitions to IdleConfig`() = runTest {
        TODO("mock access=true + initializeTtsEngineUseCase success, assert IdleConfig with installed apps + whitelist merged")
    }

    // Scenario: TTS 引擎初始化失敗（未安裝引擎）
    @Test
    fun `tts engine not installed transitions to EngineError`() = runTest {
        TODO("mock initializeTtsEngineUseCase() = Result.failure(TTS_ENGINE_NOT_INSTALLED), assert EngineError(reason)")
    }

    // Scenario: 使用者回到前景時重新檢查權限（異常矩陣：中途關閉權限）
    @Test
    fun `OnScreenResumed with revoked permission force-switches to PermissionDenied`() = runTest {
        TODO("start at IdleConfig, send OnScreenResumed with access=false, assert immediate PermissionDenied")
    }

    // Scenario: 使用者勾選白名單 App
    @Test
    fun `OnAppWhitelistToggled invokes SetAppWhitelistedUseCase and updates isChecked`() = runTest {
        TODO("send OnAppWhitelistToggled(pkg, true), verify setAppWhitelistedUseCase(pkg, true) invoked, assert item.isChecked == true")
    }

    // Scenario: 背景開始播報時前景同步顯示 TtsPlaying
    @Test
    fun `playback Speaking state transitions to TtsPlaying`() = runTest {
        TODO("emit PlaybackState.Speaking(appLabel) from observePlaybackStateUseCase, assert TtsPlaying(speakingFromLabel contains appLabel)")
    }

    // Scenario: 播報結束後回到 IdleConfig
    @Test
    fun `playback Idle state returns to IdleConfig`() = runTest {
        TODO("after Speaking, emit PlaybackState.Idle, assert uiState returns to IdleConfig")
    }

    // Scenario: 使用者按下發送測試通知
    @Test
    fun `OnSendTestNotificationClicked invokes SendTestNotificationUseCase exactly once`() = runTest {
        TODO("send OnSendTestNotificationClicked, verify(exactly = 1) { sendTestNotificationUseCase() }")
    }

    // Scenario: 使用者於 EngineError 狀態按下重試並成功恢復
    @Test
    fun `OnRetryEngineInitClicked recovers from EngineError to IdleConfig`() = runTest {
        TODO("start at EngineError, send OnRetryEngineInitClicked with initializeTtsEngineUseCase success, assert IdleConfig")
    }
}
