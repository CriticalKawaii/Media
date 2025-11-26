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
}
