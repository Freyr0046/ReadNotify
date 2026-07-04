package com.freyr.readmynotify.domain.usecase.impl

import com.freyr.readmynotify.domain.repository.NotificationWhitelistRepository
import com.freyr.readmynotify.domain.usecase.SetAppWhitelistedUseCase
import javax.inject.Inject

class SetAppWhitelistedUseCaseImpl @Inject constructor(
    private val repository: NotificationWhitelistRepository,
) : SetAppWhitelistedUseCase {

    override suspend fun invoke(packageName: String, enabled: Boolean): Result<Unit> =
        repository.setAppEnabled(packageName, enabled)
}
