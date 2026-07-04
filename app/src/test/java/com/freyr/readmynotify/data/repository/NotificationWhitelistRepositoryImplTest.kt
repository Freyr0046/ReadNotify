package com.freyr.readmynotify.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.freyr.readmynotify.data.datasource.WhitelistPreferencesDataSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class NotificationWhitelistRepositoryImplTest {

    @TempDir
    lateinit var tempDir: File

    private fun createRepository(): NotificationWhitelistRepositoryImpl {
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { File(tempDir, "test.preferences_pb") },
        )
        return NotificationWhitelistRepositoryImpl(WhitelistPreferencesDataSource(dataStore))
    }

    @Test
    fun `observeWhitelist emits empty set by default`() = runTest {
        val repository = createRepository()

        assertEquals(emptySet<String>(), repository.observeWhitelist().first())
    }

    @Test
    fun `setAppEnabled true adds package to whitelist`() = runTest {
        val repository = createRepository()

        repository.setAppEnabled("com.line", true)

        assertEquals(setOf("com.line"), repository.observeWhitelist().first())
    }

    @Test
    fun `setAppEnabled false removes package from whitelist`() = runTest {
        val repository = createRepository()
        repository.setAppEnabled("com.line", true)

        repository.setAppEnabled("com.line", false)

        assertEquals(emptySet<String>(), repository.observeWhitelist().first())
    }

    @Test
    fun `whitelist persists across repository instances backed by same file`() = runTest {
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { File(tempDir, "shared.preferences_pb") },
        )
        val first = NotificationWhitelistRepositoryImpl(WhitelistPreferencesDataSource(dataStore))
        first.setAppEnabled("com.facebook.katana", true)

        val second = NotificationWhitelistRepositoryImpl(WhitelistPreferencesDataSource(dataStore))

        assertEquals(setOf("com.facebook.katana"), second.observeWhitelist().first())
    }
}
