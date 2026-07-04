package com.freyr.readmynotify.data.repository

import com.freyr.readmynotify.data.datasource.WhitelistPreferencesDataSource
import com.freyr.readmynotify.domain.repository.NotificationWhitelistRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class NotificationWhitelistRepositoryImpl
    @Inject
    constructor(
        private val dataSource: WhitelistPreferencesDataSource,
    ) : NotificationWhitelistRepository {
        override fun observeWhitelist(): Flow<Set<String>> = dataSource.observeWhitelist()

        override suspend fun setAppEnabled(
            packageName: String,
            enabled: Boolean,
        ): Result<Unit> = runCatching { dataSource.setAppEnabled(packageName, enabled) }
    }
