package com.freyr.readmynotify.domain.model

sealed interface TtsEngineState {
    data object NotReady : TtsEngineState

    data object Ready : TtsEngineState

    data class Error(val reason: EngineErrorReason) : TtsEngineState
}
