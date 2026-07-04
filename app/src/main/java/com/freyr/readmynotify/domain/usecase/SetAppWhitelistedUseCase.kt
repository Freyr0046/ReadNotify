package com.freyr.readmynotify.domain.usecase

// LOCKED — do not modify during Phase 5
interface SetAppWhitelistedUseCase {
    suspend operator fun invoke(
        packageName: String,
        enabled: Boolean,
    ): Result<Unit>
}
