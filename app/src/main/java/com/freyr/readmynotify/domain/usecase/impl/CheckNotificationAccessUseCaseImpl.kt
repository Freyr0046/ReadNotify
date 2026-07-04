package com.freyr.readmynotify.domain.usecase.impl

import com.freyr.readmynotify.domain.repository.NotificationAccessRepository
import com.freyr.readmynotify.domain.usecase.CheckNotificationAccessUseCase
import javax.inject.Inject

class CheckNotificationAccessUseCaseImpl
    @Inject
    constructor(
        private val repository: NotificationAccessRepository,
    ) : CheckNotificationAccessUseCase {
        override fun invoke(): Boolean = repository.isNotificationAccessGranted()
    }
