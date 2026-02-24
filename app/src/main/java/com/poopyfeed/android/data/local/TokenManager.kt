package com.poopyfeed.android.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

@Singleton
class TokenManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        companion object {
            private val AUTH_TOKEN_KEY = stringPreferencesKey("auth_token")
        }

        val tokenFlow: Flow<String?> =
            context.dataStore.data.map { prefs ->
                prefs[AUTH_TOKEN_KEY]
            }

        suspend fun saveToken(token: String) {
            context.dataStore.edit { prefs ->
                prefs[AUTH_TOKEN_KEY] = token
            }
        }

        suspend fun getToken(): String? {
            return tokenFlow.first()
        }

        suspend fun clearToken() {
            context.dataStore.edit { prefs ->
                prefs.remove(AUTH_TOKEN_KEY)
            }
        }
    }
