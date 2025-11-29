package com.kiryusha.media.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

class AppPreferences(private val context: Context) {

    companion object {
        private val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        private val USER_ID = intPreferencesKey("user_id")
        private val SESSION_TOKEN = stringPreferencesKey("session_token")
        private val SESSION_EXPIRY = longPreferencesKey("session_expiry")
        private val REMEMBER_ME = booleanPreferencesKey("remember_me")
        private val THEME_MODE = stringPreferencesKey("theme_mode") // "light", "dark", "system"
        private val LANGUAGE = stringPreferencesKey("language")
        private val SHUFFLE_ENABLED = booleanPreferencesKey("shuffle_enabled")
        private val REPEAT_MODE = stringPreferencesKey("repeat_mode")
        private val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val NOTIFICATION_SOUND_ENABLED = booleanPreferencesKey("notification_sound_enabled")
        private val SHOW_PLAYBACK_NOTIFICATIONS = booleanPreferencesKey("show_playback_notifications")
    }

    suspend fun saveUserSession(userId: Int, rememberMe: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN] = true
            preferences[USER_ID] = userId
            preferences[REMEMBER_ME] = rememberMe

            val expiryTime = if (rememberMe) {
                System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)
            } else {
                System.currentTimeMillis() + (24L * 60 * 60 * 1000)
            }
            preferences[SESSION_EXPIRY] = expiryTime
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    fun isLoggedIn(): Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            val isLoggedIn = preferences[IS_LOGGED_IN] ?: false
            val expiryTime = preferences[SESSION_EXPIRY] ?: 0L

            isLoggedIn && System.currentTimeMillis() < expiryTime
        }

    fun getUserId(): Flow<Int?> = context.dataStore.data
        .map { preferences ->
            preferences[USER_ID]
        }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE] = if (enabled) "dark" else "light"
        }
    }

    fun isDarkTheme(): Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[THEME_MODE] == "dark"
        }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE] = mode
        }
    }

    fun getThemeMode(): Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[THEME_MODE] ?: "system" // Default to system theme
        }

    suspend fun setShuffleEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHUFFLE_ENABLED] = enabled
        }
    }

    fun isShuffleEnabled(): Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[SHUFFLE_ENABLED] ?: false
        }

    suspend fun setRepeatMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[REPEAT_MODE] = mode
        }
    }

    fun getRepeatMode(): Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[REPEAT_MODE] ?: "OFF"
        }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED] = enabled
        }
    }

    fun areNotificationsEnabled(): Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[NOTIFICATIONS_ENABLED] ?: true // Default to enabled
        }

    suspend fun setNotificationSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATION_SOUND_ENABLED] = enabled
        }
    }

    fun isNotificationSoundEnabled(): Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[NOTIFICATION_SOUND_ENABLED] ?: true // Default to enabled
        }

    suspend fun setShowPlaybackNotifications(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_PLAYBACK_NOTIFICATIONS] = enabled
        }
    }

    fun shouldShowPlaybackNotifications(): Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[SHOW_PLAYBACK_NOTIFICATIONS] ?: true // Default to enabled
        }

    suspend fun setLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[LANGUAGE] = language
        }
    }

    fun getLanguage(): Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[LANGUAGE] ?: "en" // Default to English
        }
}
