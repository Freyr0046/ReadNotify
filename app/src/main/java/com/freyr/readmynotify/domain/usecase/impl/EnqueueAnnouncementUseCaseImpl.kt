package com.freyr.readmynotify.domain.usecase.impl

import com.freyr.readmynotify.domain.model.Announcement
import com.freyr.readmynotify.domain.repository.SpeechQueueRepository
import com.freyr.readmynotify.domain.usecase.EnqueueAnnouncementUseCase
import javax.inject.Inject

class EnqueueAnnouncementUseCaseImpl
    @Inject
    constructor(
        private val repository: SpeechQueueRepository,
    ) : EnqueueAnnouncementUseCase {
        override suspend fun invoke(announcement: Announcement): Result<Unit> = repository.enqueue(announcement)
    }
