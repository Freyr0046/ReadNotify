package com.freyr.readmynotify.domain.usecase.impl

import com.freyr.readmynotify.domain.model.PlaybackState
import com.freyr.readmynotify.domain.repository.SpeechQueueRepository
import com.freyr.readmynotify.domain.usecase.ObservePlaybackStateUseCase
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class ObservePlaybackStateUseCaseImpl @Inject constructor(
    private val repository: SpeechQueueRepository,
) : ObservePlaybackStateUseCase {

    override fun invoke(): StateFlow<PlaybackState> = repository.playbackState
}
