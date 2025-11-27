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

    private val _isCurrentTrackFavorite = MutableStateFlow(false)
    val isCurrentTrackFavorite: StateFlow<Boolean> = _isCurrentTrackFavorite.asStateFlow()

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

        // Observe media item changes from controller
        viewModelScope.launch {
            playerController.currentMediaItemIndex.collect { index ->
                val currentPlaylist = _playlist.value
                if (currentPlaylist.isNotEmpty() && index < currentPlaylist.size && index >= 0) {
                    _currentIndex.value = index
                    val track = currentPlaylist[index]
                    _currentTrack.value = track

                    // Update favorite status
                    try {
                        if (userId != -1) {
                            _isCurrentTrackFavorite.value = musicRepository.isFavorite(userId, track.trackId)
                        } else {
                            _isCurrentTrackFavorite.value = false
                        }
                    } catch (e: Exception) {
                        _isCurrentTrackFavorite.value = false
                    }

                    // Record play in history
                    try {
                        if (userId != -1) {
                            musicRepository.recordPlay(track.trackId, userId)
                        }
                    } catch (e: Exception) {
                        // Silently ignore playback recording errors
                    }
                }
            }
        }
    }

    fun setUserId(id: Int) {
        userId = id
        // Re-check favorite status for current track when user changes
        viewModelScope.launch {
            try {
                _currentTrack.value?.let { track ->
                    if (id != -1) {
                        _isCurrentTrackFavorite.value = musicRepository.isFavorite(id, track.trackId)
                    } else {
                        _isCurrentTrackFavorite.value = false
                    }
                }
            } catch (e: Exception) {
                // If favorite status check fails, default to false
                _isCurrentTrackFavorite.value = false
            }
        }
    }

    fun playTrack(track: Track) {
        viewModelScope.launch {
            _currentTrack.value = track
            _progress.value = 0f
            _currentPosition.value = 0L
            _playerState.value = PlayerState.Playing

            // Check if track is favorite
            try {
                if (userId != -1) {
                    _isCurrentTrackFavorite.value = musicRepository.isFavorite(userId, track.trackId)
                } else {
                    _isCurrentTrackFavorite.value = false
                }
            } catch (e: Exception) {
                // If favorite status check fails, default to false
                _isCurrentTrackFavorite.value = false
            }

            // Play through controller
            playerController.playTrack(track)

            // Record play in history
            try {
                if (userId != -1) {
                    musicRepository.recordPlay(track.trackId, userId)
                }
            } catch (e: Exception) {
                // Silently ignore playback recording errors
            }
        }
    }

    fun setPlaylist(tracks: List<Track>, startIndex: Int = 0) {
        _playlist.value = tracks
        _currentIndex.value = startIndex

        // Set playlist in player controller
        playerController.setPlaylist(tracks, startIndex)

        // Apply current shuffle and repeat modes to the player
        playerController.setShuffleMode(_shuffleEnabled.value)
        val exoPlayerRepeatMode = when (_repeatMode.value) {
            RepeatMode.OFF -> androidx.media3.common.Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> androidx.media3.common.Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> androidx.media3.common.Player.REPEAT_MODE_ONE
        }
        playerController.setRepeatMode(exoPlayerRepeatMode)

        if (tracks.isNotEmpty() && startIndex < tracks.size) {
            _currentTrack.value = tracks[startIndex]
            // Check favorite status for initial track
            viewModelScope.launch {
                try {
                    if (userId != -1) {
                        _isCurrentTrackFavorite.value = musicRepository.isFavorite(userId, tracks[startIndex].trackId)
                    } else {
                        _isCurrentTrackFavorite.value = false
                    }
                } catch (e: Exception) {
                    // If favorite status check fails, default to false
                    _isCurrentTrackFavorite.value = false
                }
            }
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
        // Use player's built-in next functionality to preserve playlist
        playerController.skipToNext()
    }

    fun skipPrevious() {
        // If more than 3 seconds played, restart current track
        if (_currentPosition.value > 3000) {
            seekTo(0L)
            return
        }

        // Use player's built-in previous functionality to preserve playlist
        playerController.skipToPrevious()
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
        playerController.setShuffleMode(_shuffleEnabled.value)
    }

    fun cycleRepeatMode() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }

        // Apply repeat mode to ExoPlayer
        val exoPlayerRepeatMode = when (_repeatMode.value) {
            RepeatMode.OFF -> androidx.media3.common.Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> androidx.media3.common.Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> androidx.media3.common.Player.REPEAT_MODE_ONE
        }
        playerController.setRepeatMode(exoPlayerRepeatMode)
    }

    fun removeTrackFromQueue(track: Track) {
        val currentPlaylist = _playlist.value.toMutableList()
        val trackIndex = currentPlaylist.indexOf(track)

        if (trackIndex == -1) return

        // Remove from player controller first
        playerController.removeTrackFromQueue(trackIndex)

        // If it's the currently playing track, it will automatically skip to next
        // But we need to update our current track
        if (trackIndex == _currentIndex.value) {
            if (currentPlaylist.size > 1) {
                // Player will move to next track
                val nextIndex = if (trackIndex < currentPlaylist.size - 1) trackIndex else 0
                currentPlaylist.removeAt(trackIndex)
                _currentIndex.value = nextIndex.coerceAtMost(currentPlaylist.size - 1)
                if (currentPlaylist.isNotEmpty()) {
                    _currentTrack.value = currentPlaylist[_currentIndex.value]
                }
            } else {
                // Last track in queue
                _currentTrack.value = null
            }
        } else {
            // Adjust current index if needed
            if (trackIndex < _currentIndex.value) {
                _currentIndex.value = _currentIndex.value - 1
            }
        }

        // Remove the track from our playlist
        currentPlaylist.removeAt(trackIndex)
        _playlist.value = currentPlaylist
    }

    fun addTrackToQueue(track: Track) {
        val currentPlaylist = _playlist.value.toMutableList()
        currentPlaylist.add(track)
        _playlist.value = currentPlaylist

        // Add to player controller
        playerController.addTrackToQueue(track)
    }

    fun addTracksToQueue(tracks: List<Track>) {
        val currentPlaylist = _playlist.value.toMutableList()
        currentPlaylist.addAll(tracks)
        _playlist.value = currentPlaylist

        // Add to player controller
        playerController.addTracksToQueue(tracks)
    }

    fun addTrackNext(track: Track) {
        val currentPlaylist = _playlist.value.toMutableList()
        val insertPosition = _currentIndex.value + 1
        currentPlaylist.add(insertPosition, track)
        _playlist.value = currentPlaylist

        // Add to player controller
        playerController.addTrackNext(track)
    }

    fun addTracksNext(tracks: List<Track>) {
        val currentPlaylist = _playlist.value.toMutableList()
        val insertPosition = _currentIndex.value + 1
        currentPlaylist.addAll(insertPosition, tracks)
        _playlist.value = currentPlaylist

        // Add to player controller
        playerController.addTracksNext(tracks)
    }

    fun toggleFavorite(trackId: Long, isFavorite: Boolean) {
        if (userId == -1) return
        viewModelScope.launch {
            try {
                musicRepository.toggleFavorite(userId, trackId, !isFavorite)
                // Update current track favorite status if it's the track being toggled
                if (_currentTrack.value?.trackId == trackId) {
                    _isCurrentTrackFavorite.value = !isFavorite
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

