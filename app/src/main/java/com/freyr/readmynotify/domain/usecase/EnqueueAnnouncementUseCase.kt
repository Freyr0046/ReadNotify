package com.freyr.readmynotify.domain.usecase

import com.freyr.readmynotify.domain.model.Announcement

// LOCKED — do not modify during Phase 5
interface EnqueueAnnouncementUseCase {
    suspend operator fun invoke(announcement: Announcement): Result<Unit>
}
