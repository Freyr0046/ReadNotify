package com.freyr.readmynotify.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freyr.readmynotify.domain.model.EngineErrorReason
import com.freyr.readmynotify.domain.model.InstalledApp
import com.freyr.readmynotify.domain.model.TtsEngineState
import com.freyr.readmynotify.domain.usecase.CheckNotificationAccessUseCase
import com.freyr.readmynotify.domain.usecase.GetInstalledAppsUseCase
import com.freyr.readmynotify.domain.usecase.InitializeTtsEngineUseCase
import com.freyr.readmynotify.domain.usecase.ObserveEngineStateUseCase
import com.freyr.readmynotify.domain.usecase.ObservePlaybackStateUseCase
import com.freyr.readmynotify.domain.usecase.ObserveWhitelistUseCase
import com.freyr.readmynotify.domain.usecase.SendTestNotificationUseCase
import com.freyr.readmynotify.domain.usecase.SetAppWhitelistedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * LOCKED constructor contract — do not modify during Phase 5.
 * Method bodies are TODO() and must be implemented (not deleted or
 * reshaped) in order to satisfy MainViewModelTest
 * (see doc/phase3-contract-lock.md for the Gherkin scenarios this maps to).
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val checkNotificationAccessUseCase: CheckNotificationAccessUseCase,
    private val initializeTtsEngineUseCase: InitializeTtsEngineUseCase,
    private val observeEngineStateUseCase: ObserveEngineStateUseCase,
    private val observePlaybackStateUseCase: ObservePlaybackStateUseCase,
    private val getInstalledAppsUseCase: GetInstalledAppsUseCase,
    private val observeWhitelistUseCase: ObserveWhitelistUseCase,
    private val setAppWhitelistedUseCase: SetAppWhitelistedUseCase,
    private val sendTestNotificationUseCase: SendTestNotificationUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.InitChecking)
    val uiState: StateFlow<MainUiState> = _uiState

    private var installedApps: List<InstalledApp> = emptyList()
    private var whitelist: Set<String> = emptySet()

    init {
        runInitialCheck()
    }

    fun onIntent(intent: MainViewIntent) {
        when (intent) {
            // 由 UI 層直接開啟系統設定頁 / Play 商店頁，ViewModel 不需處理
            is MainViewIntent.OnPermissionSettingsClicked -> Unit
            is MainViewIntent.OnInstallTtsEngineClicked -> Unit

            is MainViewIntent.OnScreenResumed -> onScreenResumed()
            is MainViewIntent.OnRetryEngineInitClicked -> runInitialCheck()
            is MainViewIntent.OnAppWhitelistToggled -> Unit // Task 9 補完
            is MainViewIntent.OnSendTestNotificationClicked -> Unit // Task 9 補完
        }
    }

    /** 異常矩陣：使用者中途關閉通知權限，回到前景須立即偵測並阻擋。 */
    private fun onScreenResumed() {
        if (!checkNotificationAccessUseCase()) {
            _uiState.value = MainUiState.PermissionDenied
            return
        }
        if (_uiState.value == MainUiState.PermissionDenied) {
            runInitialCheck()
        }
    }

    private fun runInitialCheck() {
        viewModelScope.launch {
            if (!checkNotificationAccessUseCase()) {
                _uiState.value = MainUiState.PermissionDenied
                return@launch
            }

            val initResult = initializeTtsEngineUseCase()
            if (initResult.isFailure) {
                val reason = (observeEngineStateUseCase().value as? TtsEngineState.Error)?.reason
                    ?: EngineErrorReason.INIT_FAILED
                _uiState.value = MainUiState.EngineError(reason)
                return@launch
            }

            installedApps = getInstalledAppsUseCase().getOrDefault(emptyList())
            whitelist = observeWhitelistUseCase().firstOrNull() ?: emptySet()
            _uiState.value = MainUiState.IdleConfig(installedApps = buildWhitelistItems())
        }
    }

    private fun buildWhitelistItems(): List<AppWhitelistItem> = installedApps.map { app ->
        AppWhitelistItem(
            packageName = app.packageName,
            appLabel = app.label,
            isChecked = whitelist.contains(app.packageName),
        )
    }
}
