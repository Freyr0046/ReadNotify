package com.freyr.readmynotify.data.repository

import com.freyr.readmynotify.domain.model.Announcement
import com.freyr.readmynotify.domain.model.PlaybackState
import com.freyr.readmynotify.domain.repository.TtsEngineRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SpeechQueueRepositoryImplTest {
    @Test
    fun `enqueue beyond capacity of 5 drops the newest announcement`() =
        runTest(UnconfinedTestDispatcher()) {
            val ttsEngineRepository = mockk<TtsEngineRepository>()
            val releaseFirstSpeak = CompletableDeferred<Unit>()
            var speakCallCount = 0
            coEvery { ttsEngineRepository.speak(any(), any()) } coAnswers {
                speakCallCount++
                if (speakCallCount == 1) releaseFirstSpeak.await()
                Result.success(Unit)
            }
            val repository = SpeechQueueRepositoryImpl(ttsEngineRepository, backgroundScope)

            // Under UnconfinedTestDispatcher the consumer eagerly picks up the very first
            // enqueue and blocks inside speak(); that one item is now "in flight" (out of
            // the channel's buffer). The next 6 enqueues then compete for the 5-slot buffer
            // — exactly 1 of them must be silently dropped (6 offered, 5 fit).
            repository.enqueue(Announcement("App", "msg0"))
            repeat(6) { i -> repository.enqueue(Announcement("App", "msg${i + 1}")) }

            releaseFirstSpeak.complete(Unit)
            advanceUntilIdle()

            // 1 (in-flight) + 5 (buffer capacity) = 6 processed out of the 7 attempted.
            coVerify(exactly = 6) { ttsEngineRepository.speak(any(), any()) }
        }

    @Test
    fun `playbackState returns to Idle after the queue drains`() =
        runTest(UnconfinedTestDispatcher()) {
            val ttsEngineRepository = mockk<TtsEngineRepository>()
            coEvery { ttsEngineRepository.speak(any(), any()) } returns Result.success(Unit)
            val repository = SpeechQueueRepositoryImpl(ttsEngineRepository, backgroundScope)

            assertEquals(PlaybackState.Idle, repository.playbackState.value)

            repository.enqueue(Announcement("Line", "hello"))
            advanceUntilIdle()

            assertEquals(PlaybackState.Idle, repository.playbackState.value)
            coVerify { ttsEngineRepository.speak("hello", any()) }
        }

    @Test
    fun `playbackState reflects Speaking while an announcement is in flight`() =
        runTest(UnconfinedTestDispatcher()) {
            val ttsEngineRepository = mockk<TtsEngineRepository>()
            val speakStarted = CompletableDeferred<Unit>()
            val releaseSpeak = CompletableDeferred<Unit>()
            coEvery { ttsEngineRepository.speak(any(), any()) } coAnswers {
                speakStarted.complete(Unit)
                releaseSpeak.await()
                Result.success(Unit)
            }
            val repository = SpeechQueueRepositoryImpl(ttsEngineRepository, backgroundScope)

            repository.enqueue(Announcement("Line", "hello"))
            speakStarted.await()

            assertEquals(PlaybackState.Speaking("Line"), repository.playbackState.value)

            releaseSpeak.complete(Unit)
            advanceUntilIdle()

            assertEquals(PlaybackState.Idle, repository.playbackState.value)
        }
}
