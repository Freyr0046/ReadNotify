package com.freyr.readmynotify.domain.usecase.impl

import com.freyr.readmynotify.domain.repository.TestNotificationRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SendTestNotificationUseCaseImplTest {
    @Test
    fun `invoke waits 2 seconds before posting the test notification`() =
        runTest {
            val repository = mockk<TestNotificationRepository>()
            coEvery { repository.postTestNotification() } returns Result.success(Unit)
            val useCase = SendTestNotificationUseCaseImpl(repository)

            val result = useCase()

            assertEquals(2000L, currentTime)
            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { repository.postTestNotification() }
        }

    @Test
    fun `invoke propagates repository failure`() =
        runTest {
            val repository = mockk<TestNotificationRepository>()
            val failure = IllegalStateException("no permission")
            coEvery { repository.postTestNotification() } returns Result.failure(failure)
            val useCase = SendTestNotificationUseCaseImpl(repository)

            val result = useCase()

            assertTrue(result.isFailure)
            assertEquals(failure, result.exceptionOrNull())
        }
}
