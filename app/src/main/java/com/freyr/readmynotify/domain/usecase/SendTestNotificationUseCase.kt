package com.freyr.readmynotify.domain.usecase

// LOCKED — do not modify during Phase 5
interface SendTestNotificationUseCase {
    suspend operator fun invoke(): Result<Unit>
}
