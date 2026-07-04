package com.freyr.readmynotify.domain.usecase.impl

import com.freyr.readmynotify.domain.model.InstalledApp
import com.freyr.readmynotify.domain.repository.InstalledAppRepository
import com.freyr.readmynotify.domain.usecase.GetInstalledAppsUseCase
import javax.inject.Inject

class GetInstalledAppsUseCaseImpl @Inject constructor(
    private val repository: InstalledAppRepository,
) : GetInstalledAppsUseCase {

    override suspend fun invoke(): Result<List<InstalledApp>> = repository.getInstalledApps()
}
