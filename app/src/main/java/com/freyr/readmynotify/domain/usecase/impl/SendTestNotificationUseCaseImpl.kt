package com.freyr.readmynotify.domain.usecase.impl

import com.freyr.readmynotify.domain.repository.TestNotificationRepository
import com.freyr.readmynotify.domain.usecase.SendTestNotificationUseCase
import kotlinx.coroutines.delay
import javax.inject.Inject

class SendTestNotificationUseCaseImpl @Inject constructor(
    private val repository: TestNotificationRepository,
) : SendTestNotificationUseCase {

    override suspend fun invoke(): Result<Unit> {
        delay(TEST_NOTIFICATION_DELAY_MILLIS)
        return repository.postTestNotification()
    }

    private companion object {
        const val TEST_NOTIFICATION_DELAY_MILLIS = 2000L
    }
}
