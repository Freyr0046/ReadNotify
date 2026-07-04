package com.freyr.readmynotify.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InstalledAppRepositoryImplTest {

    @Test
    fun `getInstalledApps returns apps sorted by label`() = runTest {
        val context = mockk<Context>()
        val packageManager = mockk<PackageManager>()
        every { context.packageManager } returns packageManager

        val appB = ApplicationInfo().apply { packageName = "com.b" }
        val appA = ApplicationInfo().apply { packageName = "com.a" }
        every {
            packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        } returns listOf(appB, appA)
        every { packageManager.getApplicationLabel(appB) } returns "Bravo"
        every { packageManager.getApplicationLabel(appA) } returns "Alpha"

        val repository = InstalledAppRepositoryImpl(context)

        val result = repository.getInstalledApps()

        assertTrue(result.isSuccess)
        assertEquals(listOf("com.a", "com.b"), result.getOrThrow().map { it.packageName })
        assertEquals(listOf("Alpha", "Bravo"), result.getOrThrow().map { it.label })
    }

    @Test
    fun `getInstalledApps wraps exceptions in Result failure`() = runTest {
        val context = mockk<Context>()
        val packageManager = mockk<PackageManager>()
        every { context.packageManager } returns packageManager
        every {
            packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        } throws RuntimeException("boom")

        val repository = InstalledAppRepositoryImpl(context)

        val result = repository.getInstalledApps()

        assertTrue(result.isFailure)
    }
}
