package com.kiryusha.media.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiryusha.media.database.entities.Album
import com.kiryusha.media.database.entities.Track
import com.kiryusha.media.repository.MusicRepository
import com.kiryusha.media.utils.MediaScanner
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class LibraryViewModel(
    val musicRepository: MusicRepository,
    private val mediaScanner: MediaScanner
) : ViewModel() {

    private val _uiState = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _viewMode = MutableStateFlow(ViewMode.ALBUMS)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    val allTracks: StateFlow<List<Track>> = musicRepository.getAllTracks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val favoriteTracks: StateFlow<List<Track>> = musicRepository.getFavoriteTracks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val searchResults: StateFlow<List<Track>> = searchQuery
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(emptyList())
            } else {
                musicRepository.searchTracks(query)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums.asStateFlow()

    init {
        loadLibrary()
    }

    fun scanMediaFiles() {
        viewModelScope.launch {
            _uiState.value = LibraryUiState.Scanning
            try {
                val tracks = mediaScanner.scanAudioFiles()
                if (tracks.isEmpty()) {
                    _uiState.value = LibraryUiState.Empty
                } else {
                    musicRepository.importTracks(tracks)
                    _uiState.value = LibraryUiState.Success("Found ${tracks.size} tracks")
                    loadAlbums()
                }
            } catch (e: Exception) {
                _uiState.value = LibraryUiState.Error("Scanning failed: ${e.message}")
            }
        }
    }

    fun loadLibrary() {
        viewModelScope.launch {
            _uiState.value = LibraryUiState.Loading
            try {
                val trackCount = musicRepository.getTrackCount()
                if (trackCount == 0) {
                    _uiState.value = LibraryUiState.Empty
                } else {
                    loadAlbums()
                    _uiState.value = LibraryUiState.Loaded(trackCount)
                }
            } catch (e: Exception) {
                _uiState.value = LibraryUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun loadAlbums() {
        viewModelScope.launch {
            try {
                val albumList = musicRepository.getAllAlbums()
                _albums.value = albumList
            } catch (e: Exception) {
                _uiState.value = LibraryUiState.Error("Error loading albums: ${e.message}")
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun cycleViewMode() {
        _viewMode.value = when (_viewMode.value) {
            ViewMode.ALBUMS -> ViewMode.TRACKS
            ViewMode.TRACKS -> ViewMode.ARTISTS
            ViewMode.ARTISTS -> ViewMode.ALBUMS
        }
    }

    fun toggleFavorite(trackId: Long, isFavorite: Boolean) {
        viewModelScope.launch {
            try {
                musicRepository.toggleFavorite(trackId, isFavorite)
            } catch (e: Exception) {
                _uiState.value = LibraryUiState.Error("Error updating favorite")
            }
        }
    }

    fun refreshLibrary() {
        loadLibrary()
    }
}

// Enhanced PlayerViewModel with favorite toggle
class EnhancedPlayerViewModel(
    private val musicRepository: MusicRepository,
    private val playerController: com.kiryusha.media.utils.MusicPlayerController
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
    private var progressUpdateJob: kotlinx.coroutines.Job? = null

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

            playerController.playTrack(track)

            if (userId != -1) {
                musicRepository.recordPlay(track.trackId, userId)
            }
        }
    }

    fun setPlaylist(tracks: List<Track>, startIndex: Int = 0) {
        _playlist.value = tracks
        _currentIndex.value = startIndex
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
                // Handle error
            }
        }
    }

    private fun startProgressUpdate() {
        progressUpdateJob?.cancel()
        progressUpdateJob = viewModelScope.launch {
            while (isActive) {
                val position = playerController.getCurrentPosition()
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

                kotlinx.coroutines.delay(100)
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

