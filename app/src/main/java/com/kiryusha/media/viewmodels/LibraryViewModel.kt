package com.kiryusha.media.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiryusha.media.database.entities.Album
import com.kiryusha.media.database.entities.Track
import com.kiryusha.media.repository.MusicRepository
import com.kiryusha.media.utils.MediaScanner
import kotlinx.coroutines.flow.*
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
                _uiState.value = LibraryUiState.Error(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    private fun loadAlbums() {
        viewModelScope.launch {
            try {
                val albumList = musicRepository.getAllAlbums()
                _albums.value = albumList
            } catch (e: Exception) {
                _uiState.value = LibraryUiState.Error("Ошибка загрузки альбомов: ${e.message}")
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun loadAlbumTracks(albumName: String) {
        viewModelScope.launch {
            try {
                val album = musicRepository.getAlbumWithTracks(albumName)
            } catch (e: Exception) {
                _uiState.value = LibraryUiState.Error("Failed to load album")
            }
        }
    }

    fun toggleFavorite(trackId: Long, isFavorite: Boolean) {
        viewModelScope.launch {
            try {
                musicRepository.toggleFavorite(trackId, isFavorite)
            } catch (e: Exception) {
                _uiState.value = LibraryUiState.Error("Ошибка обновления избранного")
            }
        }
    }

    fun refreshLibrary() {
        loadLibrary()
    }
}

