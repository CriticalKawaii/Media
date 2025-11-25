package com.kiryusha.media.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiryusha.media.database.entities.Track
import com.kiryusha.media.repository.MusicRepository
import com.kiryusha.media.utils.MusicPlayerController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlayerViewModel(
    private val musicRepository: MusicRepository,
    private val playerController: MusicPlayerController
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
    private var progressUpdateJob: Job? = null

    init {
        // Observe player state from controller
        viewModelScope.launch {
            playerController.isPlaying.collect { playing ->
                _isPlaying.value = playing
                if (playing) {
                    startProgressUpdate()
                } else {
                    stopProgressUpdate()
                }
            }
        }
    }

    fun setUserId(id: Int) {
        userId = id
    }

    fun playTrack(track: Track) {
        viewModelScope.launch {
            _currentTrack.value = track
            _progress.value = 0f
            _currentPosition.value = 0L
            _playerState.value = PlayerState.Playing

            // Play through controller
            playerController.playTrack(track)

            // Record play in history
            if (userId != -1) {
                musicRepository.recordPlay(track.trackId, userId)
            }
        }
    }

    fun setPlaylist(tracks: List<Track>, startIndex: Int = 0) {
        _playlist.value = tracks
        _currentIndex.value = startIndex

        // Set playlist in player controller
        playerController.setPlaylist(tracks, startIndex)

        if (tracks.isNotEmpty() && startIndex < tracks.size) {
            _currentTrack.value = tracks[startIndex]
        }
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            playerController.pause()
        } else {
            playerController.play()
        }
    }

    fun skipNext() {
        viewModelScope.launch {
            val currentPlaylist = _playlist.value
            if (currentPlaylist.isEmpty()) return@launch

            val nextIndex = when {
                _shuffleEnabled.value -> {
                    val availableIndices = currentPlaylist.indices.filter { it != _currentIndex.value }
                    if (availableIndices.isEmpty()) {
                        currentPlaylist.indices.random()
                    } else {
                        availableIndices.random()
                    }
                }
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
        playerController.seekTo(position)
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

    fun toggleFavorite(trackId: Long, isFavorite: Boolean) {
        viewModelScope.launch {
            try {
                musicRepository.toggleFavorite(trackId, !isFavorite)
                // Update current track if it's the one being favorited
                _currentTrack.value?.let { track ->
                    if (track.trackId == trackId) {
                        _currentTrack.value = track.copy(isFavorite = !isFavorite)
                    }
                }
            } catch (e: Exception) {
                // Handle error silently for now
            }
        }
    }

    private fun setupPlayerListener() {
        viewModelScope.launch {
            playerController.isPlaying.collect { playing ->
                _isPlaying.value = playing
                if (playing) {
                    startProgressUpdate()
                } else {
                    stopProgressUpdate()
                }
            }
        }
    }

    private fun startProgressUpdate() {
        progressUpdateJob?.cancel()
        progressUpdateJob = viewModelScope.launch {
            while (isActive) {
                val position = playerController.getCurrentPosition()
                val duration = playerController.getDuration()

                _currentPosition.value = position

                _currentTrack.value?.let { track ->
                    _progress.value = if (track.durationMs > 0) {
                        position.toFloat() / track.durationMs.toFloat()
                    } else {
                        0f
                    }

                    if (position >= track.durationMs - 500 && track.durationMs > 0) {
                        when (_repeatMode.value) {
                            RepeatMode.ONE -> seekTo(0L)
                            else -> skipNext()
                        }
                    }
                }

                delay(100)
            }
        }
    }

    private fun stopProgressUpdate() {
        progressUpdateJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        stopProgressUpdate()
    }
}

