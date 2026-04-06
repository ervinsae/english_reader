package com.ervinzhang.englishreader.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ervinzhang.englishreader.core.model.Session
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore by preferencesDataStore(name = "session")

class SessionStore(
    private val context: Context,
) {
    private val tokenKey = stringPreferencesKey("token")
    private val userIdKey = stringPreferencesKey("user_id")
    private val expiredAtKey = longPreferencesKey("expired_at")
    private val isLoggedInKey = booleanPreferencesKey("is_logged_in")

    val session: Flow<Session?> = context.sessionDataStore.data.map { prefs ->
        val isLoggedIn = prefs[isLoggedInKey] ?: false
        val token = prefs[tokenKey]
        val userId = prefs[userIdKey]
        val expiredAt = prefs[expiredAtKey]

        if (!isLoggedIn || token == null || userId == null || expiredAt == null) {
            null
        } else {
            Session(
                token = token,
                userId = userId,
                expiredAt = expiredAt,
            )
        }
    }

    val isLoggedIn: Flow<Boolean> = session.map { it != null }

    suspend fun saveSession(session: Session) {
        context.sessionDataStore.edit { prefs ->
            prefs[tokenKey] = session.token
            prefs[userIdKey] = session.userId
            prefs[expiredAtKey] = session.expiredAt
            prefs[isLoggedInKey] = true
        }
    }

    suspend fun clearSession() {
        context.sessionDataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
