package com.freyr.readmynotify.domain.repository

import kotlinx.coroutines.flow.Flow

// LOCKED — do not modify during Phase 5
interface NotificationWhitelistRepository {
    fun observeWhitelist(): Flow<Set<String>>
    suspend fun setAppEnabled(packageName: String, enabled: Boolean): Result<Unit>
}
