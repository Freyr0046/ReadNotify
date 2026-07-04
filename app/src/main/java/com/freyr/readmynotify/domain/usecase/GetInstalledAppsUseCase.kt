package com.freyr.readmynotify.domain.usecase

import com.freyr.readmynotify.domain.model.InstalledApp

// LOCKED — do not modify during Phase 5
interface GetInstalledAppsUseCase {
    suspend operator fun invoke(): Result<List<InstalledApp>>
}
