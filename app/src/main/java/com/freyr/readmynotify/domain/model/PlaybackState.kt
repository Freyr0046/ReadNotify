package com.freyr.readmynotify.domain.model

sealed interface PlaybackState {
    data object Idle : PlaybackState
    data class Speaking(val appLabel: String) : PlaybackState
}
