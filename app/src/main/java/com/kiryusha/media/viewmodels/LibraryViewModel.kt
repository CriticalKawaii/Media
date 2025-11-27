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
import kotlinx.coroutines.flow.combine

class LibraryViewModel(
    val musicRepository: MusicRepository,
    private val mediaScanner: MediaScanner
) : ViewModel() {

    private val _uiState = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _viewMode = MutableStateFlow(ViewMode.TRACKS)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    private var currentUserId: Int = -1
    private val _userIdFlow = MutableStateFlow(-1)

    val allTracks: StateFlow<List<Track>> = _userIdFlow
        .flatMapLatest { userId ->
            if (userId == -1) {
                flowOf(emptyList())
            } else {
                musicRepository.getUserTracks(userId)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val favoriteTracks: StateFlow<List<Track>> = _userIdFlow
        .flatMapLatest { userId ->
            if (userId == -1) {
                flowOf(emptyList())
            } else {
                musicRepository.getFavoriteTracks(userId)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val searchResults: StateFlow<List<Track>> = combine(searchQuery, _userIdFlow) { query, userId ->
        Pair(query, userId)
    }
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { (query, userId) ->
            if (query.isBlank() || userId == -1) {
                flowOf(emptyList())
            } else {
                musicRepository.searchTracks(userId, query)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums.asStateFlow()

    private val _availableTracks = MutableStateFlow<List<Track>>(emptyList())
    val availableTracks: StateFlow<List<Track>> = _availableTracks.asStateFlow()

    private val _showTrackSelection = MutableStateFlow(false)
    val showTrackSelection: StateFlow<Boolean> = _showTrackSelection.asStateFlow()

    init {
        loadLibrary()
    }

    fun setUserId(userId: Int) {
        currentUserId = userId
        _userIdFlow.value = userId
    }

    fun scanMediaFiles() {
        viewModelScope.launch {
            _uiState.value = LibraryUiState.Scanning
            try {
                val tracks = mediaScanner.scanAudioFiles()
                if (tracks.isEmpty()) {
                    _uiState.value = LibraryUiState.Empty
                } else {
                    // Get existing track file paths to filter out duplicates
                    val existingPaths = allTracks.value.map { it.filePath }.toSet()
                    val newTracks = tracks.filter { it.filePath !in existingPaths }

                    if (newTracks.isEmpty()) {
                        _uiState.value = LibraryUiState.Success("All tracks are already in your library")
                    } else {
                        _availableTracks.value = newTracks
                        _showTrackSelection.value = true
                        _uiState.value = LibraryUiState.Success("Found ${newTracks.size} new tracks")
                    }
                }
            } catch (e: Exception) {
                _uiState.value = LibraryUiState.Error("Scanning failed: ${e.message}")
            }
        }
    }

    fun importSelectedTracks(selectedTracks: List<Track>) {
        if (currentUserId == -1) return
        viewModelScope.launch {
            _uiState.value = LibraryUiState.Loading
            try {
                if (selectedTracks.isNotEmpty()) {
                    musicRepository.importTracks(currentUserId, selectedTracks)
                    _uiState.value = LibraryUiState.Success("Added ${selectedTracks.size} tracks to library")
                    loadAlbums()
                }
                _showTrackSelection.value = false
                _availableTracks.value = emptyList()
            } catch (e: Exception) {
                _uiState.value = LibraryUiState.Error("Import failed: ${e.message}")
            }
        }
    }

    fun cancelTrackSelection() {
        _showTrackSelection.value = false
        _availableTracks.value = emptyList()
        _uiState.value = LibraryUiState.Loaded(allTracks.value.size)
    }

    fun loadLibrary() {
        if (currentUserId == -1) return
        viewModelScope.launch {
            _uiState.value = LibraryUiState.Loading
            try {
                val trackCount = musicRepository.getTrackCount(currentUserId)
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
        if (currentUserId == -1) return
        viewModelScope.launch {
            try {
                val albumList = musicRepository.getAllAlbums(currentUserId)
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
        if (currentUserId == -1) return
        viewModelScope.launch {
            try {
                musicRepository.toggleFavorite(currentUserId, trackId, isFavorite)
            } catch (e: Exception) {
                _uiState.value = LibraryUiState.Error("Error updating favorite")
            }
        }
    }

    fun refreshLibrary() {
        loadLibrary()
    }

    fun deleteTrack(track: Track) {
        if (currentUserId == -1) return
        viewModelScope.launch {
            try {
                musicRepository.deleteTrack(currentUserId, track)
                _uiState.value = LibraryUiState.Success("Track removed from library")
                loadAlbums()
            } catch (e: Exception) {
                _uiState.value = LibraryUiState.Error("Failed to remove track: ${e.message}")
            }
        }
    }

    fun deleteTracks(tracks: List<Track>) {
        if (currentUserId == -1) return
        viewModelScope.launch {
            try {
                musicRepository.deleteTracks(currentUserId, tracks)
                _uiState.value = LibraryUiState.Success("${tracks.size} tracks removed from library")
                loadAlbums()
            } catch (e: Exception) {
                _uiState.value = LibraryUiState.Error("Failed to remove tracks: ${e.message}")
            }
        }
    }

    suspend fun getAlbumWithTracks(albumName: String): Album? {
        if (currentUserId == -1) return null
        return musicRepository.getAlbumWithTracks(currentUserId, albumName)
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
        if (userId == -1) return
        viewModelScope.launch {
            try {
                musicRepository.toggleFavorite(userId, trackId, !isFavorite)
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

