package com.freyr.readmynotify.data.repository

import android.content.Context
import android.content.pm.PackageManager
import com.freyr.readmynotify.domain.model.InstalledApp
import com.freyr.readmynotify.domain.repository.InstalledAppRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class InstalledAppRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : InstalledAppRepository {
        override suspend fun getInstalledApps(): Result<List<InstalledApp>> =
            runCatching {
                withContext(Dispatchers.IO) {
                    val packageManager = context.packageManager
                    packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                        .map { appInfo ->
                            InstalledApp(
                                packageName = appInfo.packageName,
                                label = packageManager.getApplicationLabel(appInfo).toString(),
                            )
                        }
                        .sortedBy { it.label.lowercase() }
                }
            }
    }
