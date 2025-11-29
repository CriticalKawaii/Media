package com.kiryusha.media.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiryusha.media.utils.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _notificationSoundEnabled = MutableStateFlow(true)
    val notificationSoundEnabled: StateFlow<Boolean> = _notificationSoundEnabled.asStateFlow()

    private val _showPlaybackNotifications = MutableStateFlow(true)
    val showPlaybackNotifications: StateFlow<Boolean> = _showPlaybackNotifications.asStateFlow()

    private val _darkTheme = MutableStateFlow(false)
    val darkTheme: StateFlow<Boolean> = _darkTheme.asStateFlow()

    private val _themeMode = MutableStateFlow("system")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _language = MutableStateFlow("en")
    val language: StateFlow<String> = _language.asStateFlow()

    init {
        viewModelScope.launch {
            appPreferences.areNotificationsEnabled().collect { enabled ->
                _notificationsEnabled.value = enabled
            }
        }

        viewModelScope.launch {
            appPreferences.isNotificationSoundEnabled().collect { enabled ->
                _notificationSoundEnabled.value = enabled
            }
        }

        viewModelScope.launch {
            appPreferences.shouldShowPlaybackNotifications().collect { enabled ->
                _showPlaybackNotifications.value = enabled
            }
        }

        viewModelScope.launch {
            appPreferences.isDarkTheme().collect { enabled ->
                _darkTheme.value = enabled
            }
        }

        viewModelScope.launch {
            appPreferences.getThemeMode().collect { mode ->
                _themeMode.value = mode
            }
        }

        viewModelScope.launch {
            appPreferences.getLanguage().collect { lang ->
                _language.value = lang
            }
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setNotificationsEnabled(enabled)
        }
    }

    fun setNotificationSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setNotificationSoundEnabled(enabled)
        }
    }

    fun setShowPlaybackNotifications(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setShowPlaybackNotifications(enabled)
        }
    }

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setDarkTheme(enabled)
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            appPreferences.setThemeMode(mode)
        }
    }

    fun setLanguage(language: String) {
        viewModelScope.launch {
            appPreferences.setLanguage(language)
        }
    }
}
