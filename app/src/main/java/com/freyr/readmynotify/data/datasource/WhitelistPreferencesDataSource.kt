package com.freyr.readmynotify.data.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class WhitelistPreferencesDataSource
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) {
        fun observeWhitelist(): Flow<Set<String>> = dataStore.data.map { prefs -> prefs[WHITELIST_KEY] ?: emptySet() }

        suspend fun setAppEnabled(
            packageName: String,
            enabled: Boolean,
        ) {
            dataStore.edit { prefs ->
                val current = prefs[WHITELIST_KEY] ?: emptySet()
                prefs[WHITELIST_KEY] = if (enabled) current + packageName else current - packageName
            }
        }

        private companion object {
            val WHITELIST_KEY = stringSetPreferencesKey("notification_whitelist")
        }
    }
