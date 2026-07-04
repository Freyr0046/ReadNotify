package com.freyr.readmynotify.ui.main

import com.freyr.readmynotify.domain.model.EngineErrorReason
import com.freyr.readmynotify.domain.model.InstalledApp
import com.freyr.readmynotify.domain.model.PlaybackState
import com.freyr.readmynotify.domain.model.TtsEngineState
import com.freyr.readmynotify.domain.usecase.CheckNotificationAccessUseCase
import com.freyr.readmynotify.domain.usecase.GetInstalledAppsUseCase
import com.freyr.readmynotify.domain.usecase.InitializeTtsEngineUseCase
import com.freyr.readmynotify.domain.usecase.ObserveEngineStateUseCase
import com.freyr.readmynotify.domain.usecase.ObservePlaybackStateUseCase
import com.freyr.readmynotify.domain.usecase.ObserveWhitelistUseCase
import com.freyr.readmynotify.domain.usecase.SendTestNotificationUseCase
import com.freyr.readmynotify.domain.usecase.SetAppWhitelistedUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * LOCKED — do not modify or delete during Phase 5.
 *
 * Each test maps 1:1 to a Gherkin scenario in doc/phase3-contract-lock.md.
 * Bodies are `TODO()` on purpose — they must FAIL until Phase 5 fills in the
 * real assertions against a fully implemented [MainViewModel]. If a stub
 * seems wrong during implementation, escalate (Phase 6) — do not edit it to
 * match the implementation.
 *
 * Phase 5 addendum (Task 8/9): `viewModelScope` dispatches on
 * `Dispatchers.Main`, so a shared [StandardTestDispatcher] is installed as
 * Main via [BeforeEach]/[AfterEach] and passed into every `runTest(...)`
 * call — this is test-infrastructure wiring, not a change to what each
 * scenario asserts, and was necessary to make the locked stubs executable
 * against a real ViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val checkNotificationAccessUseCase: CheckNotificationAccessUseCase = mockk()
    private val initializeTtsEngineUseCase: InitializeTtsEngineUseCase = mockk()
    private val observeEngineStateUseCase: ObserveEngineStateUseCase = mockk()
    private val observePlaybackStateUseCase: ObservePlaybackStateUseCase = mockk()
    private val getInstalledAppsUseCase: GetInstalledAppsUseCase = mockk()
    private val observeWhitelistUseCase: ObserveWhitelistUseCase = mockk()
    private val setAppWhitelistedUseCase: SetAppWhitelistedUseCase = mockk()
    private val sendTestNotificationUseCase: SendTestNotificationUseCase = mockk()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

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
    fun `initial state is InitChecking`() = runTest(testDispatcher) {
        // checkNotificationAccessUseCase()（非 suspend）會同步跑完，但
        // initializeTtsEngineUseCase() 是真正的 suspend（等待非同步系統回呼），
        // 在它 resume 之前，UiState 應停留在 InitChecking —— 與正式環境中
        // TTS OnInitListener 尚未回呼前的行為一致。
        every { checkNotificationAccessUseCase() } returns true
        coEvery { initializeTtsEngineUseCase() } coAnswers { awaitCancellation() }

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(MainUiState.InitChecking, viewModel.uiState.value)
    }

    // Scenario: 初始化檢查後偵測到未授權通知存取權限
    @Test
    fun `permission denied transitions to PermissionDenied`() = runTest(testDispatcher) {
        every { checkNotificationAccessUseCase() } returns false

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(MainUiState.PermissionDenied, viewModel.uiState.value)
    }

    // Scenario: 初始化檢查後偵測到權限已授權且 TTS 就緒
    @Test
    fun `permission granted and tts ready transitions to IdleConfig`() = runTest(testDispatcher) {
        every { checkNotificationAccessUseCase() } returns true
        coEvery { initializeTtsEngineUseCase() } returns Result.success(Unit)
        coEvery { getInstalledAppsUseCase() } returns Result.success(
            listOf(InstalledApp("com.line", "Line"), InstalledApp("com.facebook.katana", "Facebook")),
        )
        every { observeWhitelistUseCase() } returns flowOf(setOf("com.line"))
        every { observePlaybackStateUseCase() } returns MutableStateFlow(PlaybackState.Idle)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is MainUiState.IdleConfig)
        val idleConfig = state as MainUiState.IdleConfig
        assertEquals(2, idleConfig.installedApps.size)
        assertTrue(idleConfig.installedApps.first { it.packageName == "com.line" }.isChecked)
        assertFalse(idleConfig.installedApps.first { it.packageName == "com.facebook.katana" }.isChecked)
    }

    // Scenario: TTS 引擎初始化失敗（未安裝引擎）
    @Test
    fun `tts engine not installed transitions to EngineError`() = runTest(testDispatcher) {
        every { checkNotificationAccessUseCase() } returns true
        coEvery { initializeTtsEngineUseCase() } returns Result.failure(IllegalStateException("no engine"))
        every { observeEngineStateUseCase() } returns MutableStateFlow(
            TtsEngineState.Error(EngineErrorReason.TTS_ENGINE_NOT_INSTALLED),
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(
            MainUiState.EngineError(EngineErrorReason.TTS_ENGINE_NOT_INSTALLED),
            viewModel.uiState.value,
        )
    }

    // Scenario: 使用者回到前景時重新檢查權限（異常矩陣：中途關閉權限）
    @Test
    fun `OnScreenResumed with revoked permission force-switches to PermissionDenied`() = runTest(testDispatcher) {
        every { checkNotificationAccessUseCase() } returns true
        coEvery { initializeTtsEngineUseCase() } returns Result.success(Unit)
        coEvery { getInstalledAppsUseCase() } returns Result.success(emptyList())
        every { observeWhitelistUseCase() } returns flowOf(emptySet())
        every { observePlaybackStateUseCase() } returns MutableStateFlow(PlaybackState.Idle)

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is MainUiState.IdleConfig)

        every { checkNotificationAccessUseCase() } returns false
        viewModel.onIntent(MainViewIntent.OnScreenResumed)

        assertEquals(MainUiState.PermissionDenied, viewModel.uiState.value)
    }

    // Scenario: 使用者勾選白名單 App
    @Test
    fun `OnAppWhitelistToggled invokes SetAppWhitelistedUseCase and updates isChecked`() = runTest(testDispatcher) {
        val whitelistFlow = MutableStateFlow(emptySet<String>())
        every { checkNotificationAccessUseCase() } returns true
        coEvery { initializeTtsEngineUseCase() } returns Result.success(Unit)
        coEvery { getInstalledAppsUseCase() } returns Result.success(listOf(InstalledApp("com.line", "Line")))
        every { observeWhitelistUseCase() } returns whitelistFlow
        every { observePlaybackStateUseCase() } returns MutableStateFlow(PlaybackState.Idle)
        coEvery { setAppWhitelistedUseCase("com.line", true) } coAnswers {
            whitelistFlow.value = setOf("com.line")
            Result.success(Unit)
        }

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(MainViewIntent.OnAppWhitelistToggled("com.line", true))
        advanceUntilIdle()

        coVerify { setAppWhitelistedUseCase("com.line", true) }
        val state = viewModel.uiState.value
        assertTrue(state is MainUiState.IdleConfig)
        assertTrue((state as MainUiState.IdleConfig).installedApps.first().isChecked)
    }

    // Scenario: 背景開始播報時前景同步顯示 TtsPlaying
    @Test
    fun `playback Speaking state transitions to TtsPlaying`() = runTest(testDispatcher) {
        val playbackFlow = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
        every { checkNotificationAccessUseCase() } returns true
        coEvery { initializeTtsEngineUseCase() } returns Result.success(Unit)
        coEvery { getInstalledAppsUseCase() } returns Result.success(emptyList())
        every { observeWhitelistUseCase() } returns flowOf(emptySet())
        every { observePlaybackStateUseCase() } returns playbackFlow

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is MainUiState.IdleConfig)

        playbackFlow.value = PlaybackState.Speaking("Line")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is MainUiState.TtsPlaying)
        assertTrue((state as MainUiState.TtsPlaying).speakingFromLabel.contains("Line"))
    }

    // Scenario: 播報結束後回到 IdleConfig
    @Test
    fun `playback Idle state returns to IdleConfig`() = runTest(testDispatcher) {
        val playbackFlow = MutableStateFlow<PlaybackState>(PlaybackState.Speaking("Line"))
        every { checkNotificationAccessUseCase() } returns true
        coEvery { initializeTtsEngineUseCase() } returns Result.success(Unit)
        coEvery { getInstalledAppsUseCase() } returns Result.success(emptyList())
        every { observeWhitelistUseCase() } returns flowOf(emptySet())
        every { observePlaybackStateUseCase() } returns playbackFlow

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is MainUiState.TtsPlaying)

        playbackFlow.value = PlaybackState.Idle
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is MainUiState.IdleConfig)
    }

    // Scenario: 使用者按下發送測試通知
    @Test
    fun `OnSendTestNotificationClicked invokes SendTestNotificationUseCase exactly once`() = runTest(testDispatcher) {
        every { checkNotificationAccessUseCase() } returns true
        coEvery { initializeTtsEngineUseCase() } returns Result.success(Unit)
        coEvery { getInstalledAppsUseCase() } returns Result.success(emptyList())
        every { observeWhitelistUseCase() } returns flowOf(emptySet())
        every { observePlaybackStateUseCase() } returns MutableStateFlow(PlaybackState.Idle)
        coEvery { sendTestNotificationUseCase() } returns Result.success(Unit)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(MainViewIntent.OnSendTestNotificationClicked)
        advanceUntilIdle()

        coVerify(exactly = 1) { sendTestNotificationUseCase() }
    }

    // Scenario: 使用者於 EngineError 狀態按下重試並成功恢復
    @Test
    fun `OnRetryEngineInitClicked recovers from EngineError to IdleConfig`() = runTest(testDispatcher) {
        every { checkNotificationAccessUseCase() } returns true
        coEvery { initializeTtsEngineUseCase() } returns Result.failure(IllegalStateException("no engine"))
        every { observeEngineStateUseCase() } returns MutableStateFlow(
            TtsEngineState.Error(EngineErrorReason.TTS_ENGINE_NOT_INSTALLED),
        )
        coEvery { getInstalledAppsUseCase() } returns Result.success(emptyList())
        every { observeWhitelistUseCase() } returns flowOf(emptySet())
        every { observePlaybackStateUseCase() } returns MutableStateFlow(PlaybackState.Idle)

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is MainUiState.EngineError)

        coEvery { initializeTtsEngineUseCase() } returns Result.success(Unit)
        viewModel.onIntent(MainViewIntent.OnRetryEngineInitClicked)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is MainUiState.IdleConfig)
    }
}
