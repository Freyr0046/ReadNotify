package com.freyr.readmynotify.domain.usecase.impl

import com.freyr.readmynotify.domain.repository.NotificationWhitelistRepository
import com.freyr.readmynotify.domain.usecase.ObserveWhitelistUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveWhitelistUseCaseImpl @Inject constructor(
    private val repository: NotificationWhitelistRepository,
) : ObserveWhitelistUseCase {

    override fun invoke(): Flow<Set<String>> = repository.observeWhitelist()
}
