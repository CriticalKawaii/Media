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
