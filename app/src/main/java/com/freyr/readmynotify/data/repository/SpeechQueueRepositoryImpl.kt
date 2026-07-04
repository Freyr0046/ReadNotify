package com.freyr.readmynotify.data.repository

import com.freyr.readmynotify.di.ApplicationCoroutineScope
import com.freyr.readmynotify.domain.model.Announcement
import com.freyr.readmynotify.domain.model.PlaybackState
import com.freyr.readmynotify.domain.repository.SpeechQueueRepository
import com.freyr.readmynotify.domain.repository.TtsEngineRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TextToSpeech 引擎本身不提供查詢佇列長度的 API，因此在此自行維護一個容量
 * 為 5 的 Channel：滿載時新通知以 [BufferOverflow.DROP_LATEST] 靜默捨棄
 * （PRD 異常矩陣：連續 20 條群發時，佇列最多積壓 5 條，超過直接捨棄）。
 */
@Singleton
class SpeechQueueRepositoryImpl @Inject constructor(
    private val ttsEngineRepository: TtsEngineRepository,
    @ApplicationCoroutineScope scope: CoroutineScope,
) : SpeechQueueRepository {

    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    override val playbackState: StateFlow<PlaybackState> = _playbackState

    private val channel = Channel<Announcement>(
        capacity = MAX_QUEUE_SIZE,
        onBufferOverflow = BufferOverflow.DROP_LATEST,
    )

    init {
        scope.launch {
            for (announcement in channel) {
                _playbackState.value = PlaybackState.Speaking(announcement.appLabel)
                ttsEngineRepository.speak(
                    text = announcement.message,
                    utteranceId = "${announcement.appLabel}-${announcement.hashCode()}",
                )
                _playbackState.value = PlaybackState.Idle
            }
        }
    }

    override suspend fun enqueue(announcement: Announcement): Result<Unit> {
        channel.trySend(announcement)
        return Result.success(Unit)
    }

    private companion object {
        const val MAX_QUEUE_SIZE = 5
    }
}
