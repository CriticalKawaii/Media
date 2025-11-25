package com.kiryusha.media.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiryusha.media.database.entities.User
import com.kiryusha.media.repository.MusicRepository
import com.kiryusha.media.repository.PlaylistRepository
import com.kiryusha.media.repository.UserRepository
import com.kiryusha.media.repository.UserStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userRepository: UserRepository,
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _userStats = MutableStateFlow<UserStats?>(null)
    val userStats: StateFlow<UserStats?> = _userStats.asStateFlow()

    private val _playlistCount = MutableStateFlow(0)
    val playlistCount: StateFlow<Int> = _playlistCount.asStateFlow()

    fun loadUserProfile(userId: Int) {
        viewModelScope.launch {
            try {
                val user = userRepository.getUserById(userId)
                _currentUser.value = user

                val stats = musicRepository.getUserStats(userId)
                _userStats.value = stats
            } catch (e: Exception) {
            }
        }

        viewModelScope.launch {
            playlistRepository.getUserPlaylists(userId).collect { playlists ->
                _playlistCount.value = playlists.size
            }
        }
    }

    fun formatPlaytime(totalPlays: Int): String {
        // Assume average song duration of 3.5 minutes
        val totalMinutes = (totalPlays * 3.5).toInt()
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return "${hours}h ${minutes}m"
    }
}
