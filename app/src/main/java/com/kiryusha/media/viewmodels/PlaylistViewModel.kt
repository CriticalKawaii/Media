package com.kiryusha.media.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiryusha.media.database.entities.Playlist
import com.kiryusha.media.database.entities.PlaylistWithTracks
import com.kiryusha.media.database.entities.Track
import com.kiryusha.media.repository.MusicRepository
import com.kiryusha.media.repository.PlaylistRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PlaylistViewModel(
    private val playlistRepository: PlaylistRepository,
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PlaylistUiState>(PlaylistUiState.Idle)
    val uiState: StateFlow<PlaylistUiState> = _uiState.asStateFlow()

    private val _currentPlaylist = MutableStateFlow<PlaylistWithTracks?>(null)
    val currentPlaylist: StateFlow<PlaylistWithTracks?> = _currentPlaylist.asStateFlow()

    private val _currentPlaylistTracks = MutableStateFlow<List<Track>>(emptyList())
    val currentPlaylistTracks: StateFlow<List<Track>> = _currentPlaylistTracks.asStateFlow()

    private var currentUserId: Int = -1

    private val _userIdFlow = MutableStateFlow(-1)

    private val _favoriteTracks: StateFlow<List<Track>> =
        _userIdFlow
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

    private val _userPlaylistsOnly: StateFlow<List<PlaylistWithTracks>> =
        _userIdFlow
            .flatMapLatest { userId ->
                if (userId == -1) {
                    flowOf(emptyList())
                } else {
                    playlistRepository.getUserPlaylistsWithTracks(userId)
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val userPlaylists: StateFlow<List<PlaylistWithTracks>> =
        combine(_favoriteTracks, _userPlaylistsOnly) { favorites, playlists ->
            if (favorites.isNotEmpty()) {
                val favoritePlaylist = PlaylistWithTracks(
                    playlist = Playlist(
                        playlistId = -1, // Special ID for favorites
                        name = "Favorites",
                        description = "Your favorite tracks",
                        userId = currentUserId
                    ),
                    tracks = favorites
                )
                listOf(favoritePlaylist) + playlists
            } else {
                playlists
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setUserId(userId: Int) {
        currentUserId = userId
        _userIdFlow.value = userId
    }

    fun createPlaylist(name: String, description: String? = null) {
        if (currentUserId == -1) {
            _uiState.value = PlaylistUiState.Error("Пользователь не авторизован")
            return
        }

        viewModelScope.launch {
            _uiState.value = PlaylistUiState.Loading
            try {
                val playlist = Playlist(
                    name = name,
                    description = description,
                    userId = currentUserId
                )
                val playlistId = playlistRepository.createPlaylist(playlist)
                _uiState.value = PlaylistUiState.PlaylistCreated(playlistId)
            } catch (e: Exception) {
                _uiState.value = PlaylistUiState.Error("Ошибка создания плейлиста: ${e.message}")
            }
        }
    }

    fun loadPlaylist(playlistId: Long) {
        viewModelScope.launch {
            _uiState.value = PlaylistUiState.Loading
            try {
                if (playlistId == -1L) {
                    val favorites = _favoriteTracks.value
                    val favoritesPlaylist = PlaylistWithTracks(
                        playlist = Playlist(
                            playlistId = -1,
                            name = "Favorites",
                            description = "Your favorite tracks",
                            userId = currentUserId
                        ),
                        tracks = favorites
                    )
                    _currentPlaylist.value = favoritesPlaylist
                    _currentPlaylistTracks.value = favorites
                    _uiState.value = PlaylistUiState.PlaylistLoaded(favoritesPlaylist)
                } else {
                    val playlist = playlistRepository.getPlaylistWithTracks(playlistId)
                    val orderedTracks = playlistRepository.getPlaylistTracksOrdered(playlistId)
                    _currentPlaylist.value = playlist
                    _currentPlaylistTracks.value = orderedTracks
                    _uiState.value = if (playlist != null) {
                        PlaylistUiState.PlaylistLoaded(playlist)
                    } else {
                        PlaylistUiState.Error("Плейлист не найден")
                    }
                }
            } catch (e: Exception) {
                _uiState.value = PlaylistUiState.Error("Ошибка загрузки: ${e.message}")
            }
        }
    }

    fun addTrackToPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch {
            try {
                playlistRepository.addTrackToPlaylist(playlistId, trackId)
                _uiState.value = PlaylistUiState.TrackAdded
                if (_currentPlaylist.value?.playlist?.playlistId == playlistId) {
                    loadPlaylist(playlistId)
                }
            } catch (e: Exception) {
                _uiState.value = PlaylistUiState.Error("Ошибка добавления трека: ${e.message}")
            }
        }
    }

    fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch {
            try {
                playlistRepository.removeTrackFromPlaylist(playlistId, trackId)
                _uiState.value = PlaylistUiState.TrackRemoved
                if (_currentPlaylist.value?.playlist?.playlistId == playlistId) {
                    loadPlaylist(playlistId)
                }
            } catch (e: Exception) {
                _uiState.value = PlaylistUiState.Error("Ошибка удаления трека: ${e.message}")
            }
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            _uiState.value = PlaylistUiState.Loading
            try {
                playlistRepository.deletePlaylist(playlistId)
                _uiState.value = PlaylistUiState.PlaylistDeleted
                _currentPlaylist.value = null
            } catch (e: Exception) {
                _uiState.value = PlaylistUiState.Error("Ошибка удаления: ${e.message}")
            }
        }
    }

    fun updatePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            _uiState.value = PlaylistUiState.Loading
            try {
                playlistRepository.updatePlaylist(playlist)
                _uiState.value = PlaylistUiState.PlaylistUpdated
                loadPlaylist(playlist.playlistId)
            } catch (e: Exception) {
                _uiState.value = PlaylistUiState.Error("Ошибка обновления: ${e.message}")
            }
        }
    }

    fun updateTrackPositions(playlistId: Long, trackIds: List<Long>) {
        viewModelScope.launch {
            try {
                playlistRepository.updateTrackPositions(playlistId, trackIds)
            } catch (e: Exception) {
                _uiState.value = PlaylistUiState.Error("Ошибка обновления порядка: ${e.message}")
            }
        }
    }

    fun resetState() {
        _uiState.value = PlaylistUiState.Idle
    }
}
