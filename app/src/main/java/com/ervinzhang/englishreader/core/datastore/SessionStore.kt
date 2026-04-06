package com.ervinzhang.englishreader.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore by preferencesDataStore(name = "session")

class SessionStore(
    private val context: Context,
) {
    private val tokenKey = stringPreferencesKey("token")
    private val userIdKey = stringPreferencesKey("user_id")
    private val isLoggedInKey = booleanPreferencesKey("is_logged_in")

    val isLoggedIn: Flow<Boolean> = context.sessionDataStore.data.map { prefs ->
        prefs[isLoggedInKey] ?: false
    }

    suspend fun saveSession(token: String, userId: String) {
        context.sessionDataStore.edit { prefs ->
            prefs[tokenKey] = token
            prefs[userIdKey] = userId
            prefs[isLoggedInKey] = true
        }
    }

    suspend fun clearSession() {
        context.sessionDataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
