package com.kiryusha.media.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiryusha.media.api.lyrics.LyricsRepository
import com.kiryusha.media.api.lyrics.LyricsResult
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
    private val playerController: MusicPlayerController,
    private val lyricsRepository: LyricsRepository
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

    private val _lyricsState = MutableStateFlow<LyricsResult>(LyricsResult.Loading)
    val lyricsState: StateFlow<LyricsResult> = _lyricsState.asStateFlow()

    private var userId: Int = -1
    private var progressUpdateJob: Job? = null

    init {
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

        viewModelScope.launch {
            playerController.currentMediaItemIndex.collect { index ->
                val currentPlaylist = _playlist.value
                if (currentPlaylist.isNotEmpty() && index < currentPlaylist.size && index >= 0) {
                    _currentIndex.value = index
                    val track = currentPlaylist[index]
                    _currentTrack.value = track

                    try {
                        if (userId != -1) {
                            _isCurrentTrackFavorite.value = musicRepository.isFavorite(userId, track.trackId)
                        } else {
                            _isCurrentTrackFavorite.value = false
                        }
                    } catch (e: Exception) {
                        _isCurrentTrackFavorite.value = false
                    }

                    try {
                        if (userId != -1) {
                            musicRepository.recordPlay(track.trackId, userId)
                        }
                    } catch (e: Exception) {
                    }
                }
            }
        }
    }

    fun setUserId(id: Int) {
        userId = id
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

            try {
                if (userId != -1) {
                    _isCurrentTrackFavorite.value = musicRepository.isFavorite(userId, track.trackId)
                } else {
                    _isCurrentTrackFavorite.value = false
                }
            } catch (e: Exception) {
                _isCurrentTrackFavorite.value = false
            }

            playerController.playTrack(track)

            try {
                if (userId != -1) {
                    musicRepository.recordPlay(track.trackId, userId)
                }
            } catch (e: Exception) {
            }
        }
    }

    fun setPlaylist(tracks: List<Track>, startIndex: Int = 0) {
        _playlist.value = tracks
        _currentIndex.value = startIndex

        playerController.setPlaylist(tracks, startIndex)

        playerController.setShuffleMode(_shuffleEnabled.value)
        val exoPlayerRepeatMode = when (_repeatMode.value) {
            RepeatMode.OFF -> androidx.media3.common.Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> androidx.media3.common.Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> androidx.media3.common.Player.REPEAT_MODE_ONE
        }
        playerController.setRepeatMode(exoPlayerRepeatMode)

        if (tracks.isNotEmpty() && startIndex < tracks.size) {
            _currentTrack.value = tracks[startIndex]
            viewModelScope.launch {
                try {
                    if (userId != -1) {
                        _isCurrentTrackFavorite.value = musicRepository.isFavorite(userId, tracks[startIndex].trackId)
                    } else {
                        _isCurrentTrackFavorite.value = false
                    }
                } catch (e: Exception) {
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
        playerController.skipToNext()
    }

    fun skipPrevious() {
        if (_currentPosition.value > 3000) {
            seekTo(0L)
            return
        }

        playerController.skipToPrevious()
    }

    fun skipToIndex(index: Int) {
        val currentPlaylist = _playlist.value
        if (index >= 0 && index < currentPlaylist.size) {
            _currentIndex.value = index
            playerController.skipToIndex(index)
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
        playerController.setShuffleMode(_shuffleEnabled.value)
    }

    fun cycleRepeatMode() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }

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

        playerController.removeTrackFromQueue(trackIndex)

        if (trackIndex == _currentIndex.value) {
            if (currentPlaylist.size > 1) {
                val nextIndex = if (trackIndex < currentPlaylist.size - 1) trackIndex else 0
                currentPlaylist.removeAt(trackIndex)
                _currentIndex.value = nextIndex.coerceAtMost(currentPlaylist.size - 1)
                if (currentPlaylist.isNotEmpty()) {
                    _currentTrack.value = currentPlaylist[_currentIndex.value]
                }
            } else {
                _currentTrack.value = null
            }
        } else {
            if (trackIndex < _currentIndex.value) {
                _currentIndex.value = _currentIndex.value - 1
            }
        }

        currentPlaylist.removeAt(trackIndex)
        _playlist.value = currentPlaylist
    }

    fun addTrackToQueue(track: Track) {
        val currentPlaylist = _playlist.value.toMutableList()
        currentPlaylist.add(track)
        _playlist.value = currentPlaylist

        playerController.addTrackToQueue(track)
    }

    fun addTracksToQueue(tracks: List<Track>) {
        val currentPlaylist = _playlist.value.toMutableList()
        currentPlaylist.addAll(tracks)
        _playlist.value = currentPlaylist

        playerController.addTracksToQueue(tracks)
    }

    fun addTrackNext(track: Track) {
        val currentPlaylist = _playlist.value.toMutableList()
        val insertPosition = _currentIndex.value + 1
        currentPlaylist.add(insertPosition, track)
        _playlist.value = currentPlaylist

        playerController.addTrackNext(track)
    }

    fun addTracksNext(tracks: List<Track>) {
        val currentPlaylist = _playlist.value.toMutableList()
        val insertPosition = _currentIndex.value + 1
        currentPlaylist.addAll(insertPosition, tracks)
        _playlist.value = currentPlaylist

        playerController.addTracksNext(tracks)
    }

    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        val currentPlaylist = _playlist.value.toMutableList()

        if (fromIndex >= 0 && fromIndex < currentPlaylist.size &&
            toIndex >= 0 && toIndex < currentPlaylist.size &&
            fromIndex != toIndex) {

            val movedTrack = currentPlaylist.removeAt(fromIndex)
            currentPlaylist.add(toIndex, movedTrack)
            _playlist.value = currentPlaylist

            when {
                fromIndex == _currentIndex.value -> {
                    _currentIndex.value = toIndex
                }
                fromIndex < _currentIndex.value && toIndex >= _currentIndex.value -> {
                    _currentIndex.value = _currentIndex.value - 1
                }
                fromIndex > _currentIndex.value && toIndex <= _currentIndex.value -> {
                    _currentIndex.value = _currentIndex.value + 1
                }
            }

            playerController.reorderQueue(fromIndex, toIndex)
        }
    }

    fun toggleFavorite(trackId: Long, isFavorite: Boolean) {
        if (userId == -1) return
        viewModelScope.launch {
            try {
                musicRepository.toggleFavorite(userId, trackId, !isFavorite)
                if (_currentTrack.value?.trackId == trackId) {
                    _isCurrentTrackFavorite.value = !isFavorite
                }
            } catch (e: Exception) {
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

    /**
     * Fetches lyrics for the current track
     */
    fun fetchLyrics() {
        val track = _currentTrack.value ?: return

        viewModelScope.launch {
            _lyricsState.value = LyricsResult.Loading
            val result = lyricsRepository.getLyrics(
                trackId = track.trackId,
                artist = track.artist,
                title = track.title
            )
            _lyricsState.value = result
        }
    }

    /**
     * Clears the current lyrics state
     */
    fun clearLyricsState() {
        _lyricsState.value = LyricsResult.Loading
    }

    override fun onCleared() {
        super.onCleared()
        stopProgressUpdate()
    }
}

