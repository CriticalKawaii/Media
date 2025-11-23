package com.kiryusha.media.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiryusha.media.database.entities.Track
import com.kiryusha.media.repository.MusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlayerViewModel(
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _playlist = MutableStateFlow<List<Track>>(emptyList())
    val playlist: StateFlow<List<Track>> = _playlist.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _playerState = MutableStateFlow<PlayerState>(PlayerState.Idle)
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private var userId: Int = -1

    fun setUserId(id: Int) {
        userId = id
    }

    fun playTrack(track: Track) {
        viewModelScope.launch {
            _currentTrack.value = track
            _isPlaying.value = true
            _progress.value = 0f
            _currentPosition.value = 0L
            _playerState.value = PlayerState.Playing

            // Record play in history
            if (userId != -1) {
                musicRepository.recordPlay(track.trackId, userId)
            }
        }
    }

    fun setPlaylist(tracks: List<Track>, startIndex: Int = 0) {
        _playlist.value = tracks
        _currentIndex.value = startIndex
        if (tracks.isNotEmpty() && startIndex < tracks.size) {
            playTrack(tracks[startIndex])
        }
    }

    fun togglePlayPause() {
        _isPlaying.value = !_isPlaying.value
        _playerState.value = if (_isPlaying.value) {
            PlayerState.Playing
        } else {
            PlayerState.Paused
        }
    }

    fun skipNext() {
        viewModelScope.launch {
            val currentPlaylist = _playlist.value
            if (currentPlaylist.isEmpty()) return@launch

            val nextIndex = when {
                _shuffleEnabled.value -> (0 until currentPlaylist.size).random()
                _currentIndex.value < currentPlaylist.size - 1 -> _currentIndex.value + 1
                _repeatMode.value == RepeatMode.ALL -> 0
                else -> return@launch
            }

            _currentIndex.value = nextIndex
            playTrack(currentPlaylist[nextIndex])
        }
    }

    fun skipPrevious() {
        viewModelScope.launch {
            val currentPlaylist = _playlist.value
            if (currentPlaylist.isEmpty()) return@launch

            // If more than 3 seconds played, restart current track
            if (_currentPosition.value > 3000) {
                seekTo(0L)
                return@launch
            }

            val previousIndex = when {
                _currentIndex.value > 0 -> _currentIndex.value - 1
                _repeatMode.value == RepeatMode.ALL -> currentPlaylist.size - 1
                else -> 0
            }

            _currentIndex.value = previousIndex
            playTrack(currentPlaylist[previousIndex])
        }
    }

    fun seekTo(position: Long) {
        _currentPosition.value = position
        _currentTrack.value?.let { track ->
            _progress.value = position.toFloat() / track.durationMs.toFloat()
        }
    }

    fun updateProgress(position: Long) {
        _currentPosition.value = position
        _currentTrack.value?.let { track ->
            _progress.value = position.toFloat() / track.durationMs.toFloat()
        }
    }

    fun toggleShuffle() {
        _shuffleEnabled.value = !_shuffleEnabled.value
    }

    fun cycleRepeatMode() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    fun onTrackCompleted() {
        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                // Replay current track
                playTrack(_currentTrack.value ?: return)
            }
            RepeatMode.ALL, RepeatMode.OFF -> {
                skipNext()
            }
        }
    }

    fun pause() {
        _isPlaying.value = false
        _playerState.value = PlayerState.Paused
    }

    fun resume() {
        _isPlaying.value = true
        _playerState.value = PlayerState.Playing
    }

    fun stop() {
        _isPlaying.value = false
        _progress.value = 0f
        _currentPosition.value = 0L
        _playerState.value = PlayerState.Stopped
    }
}
