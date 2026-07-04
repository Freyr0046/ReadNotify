package com.freyr.readmynotify.domain.repository

import com.freyr.readmynotify.domain.model.InstalledApp

// LOCKED — do not modify during Phase 5
interface InstalledAppRepository {
    suspend fun getInstalledApps(): Result<List<InstalledApp>>
}
