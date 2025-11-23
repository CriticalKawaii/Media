package com.kiryusha.media.viewmodels

import com.kiryusha.media.database.entities.PlaylistWithTracks

sealed class PlaylistUiState {
    object Idle : PlaylistUiState()
    object Loading : PlaylistUiState()
    data class PlaylistCreated(val playlistId: Long) : PlaylistUiState()
    data class PlaylistLoaded(val playlist: PlaylistWithTracks) : PlaylistUiState()
    object PlaylistUpdated : PlaylistUiState()
    object PlaylistDeleted : PlaylistUiState()
    object TrackAdded : PlaylistUiState()
    object TrackRemoved : PlaylistUiState()
    data class Error(val message: String) : PlaylistUiState()
}