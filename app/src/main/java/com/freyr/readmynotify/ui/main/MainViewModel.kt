package com.freyr.readmynotify.ui.main

import androidx.lifecycle.ViewModel
import com.freyr.readmynotify.domain.usecase.CheckNotificationAccessUseCase
import com.freyr.readmynotify.domain.usecase.GetInstalledAppsUseCase
import com.freyr.readmynotify.domain.usecase.InitializeTtsEngineUseCase
import com.freyr.readmynotify.domain.usecase.ObserveEngineStateUseCase
import com.freyr.readmynotify.domain.usecase.ObservePlaybackStateUseCase
import com.freyr.readmynotify.domain.usecase.ObserveWhitelistUseCase
import com.freyr.readmynotify.domain.usecase.SendTestNotificationUseCase
import com.freyr.readmynotify.domain.usecase.SetAppWhitelistedUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * LOCKED constructor contract — do not modify during Phase 5.
 * Method bodies are TODO() and must be implemented (not deleted or
 * reshaped) in order to satisfy MainViewModelTest
 * (see doc/phase3-contract-lock.md for the Gherkin scenarios this maps to).
 */
class MainViewModel(
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

    fun onIntent(intent: MainViewIntent) {
        TODO("Phase 5: implement state transitions per doc/phase3-contract-lock.md")
    }
}
