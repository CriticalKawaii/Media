package com.kiryusha.media.repository

import com.kiryusha.media.database.dao.PlaylistDao
import com.kiryusha.media.database.entities.Playlist
import com.kiryusha.media.database.entities.PlaylistWithTracks
import kotlinx.coroutines.flow.Flow

class PlaylistRepository(private val playlistDao: PlaylistDao) {

    fun getUserPlaylists(userId: Int): Flow<List<Playlist>> =
        playlistDao.getUserPlaylists(userId)

    fun getUserPlaylistsWithTracks(userId: Int): Flow<List<PlaylistWithTracks>> =
        playlistDao.getUserPlaylistsWithTracks(userId)

    suspend fun getPlaylistById(playlistId: Long): Playlist? =
        playlistDao.getPlaylistById(playlistId)

    suspend fun getPlaylistWithTracks(playlistId: Long): PlaylistWithTracks? =
        playlistDao.getPlaylistWithTracks(playlistId)

    suspend fun createPlaylist(playlist: Playlist): Long {
        return playlistDao.insertPlaylist(playlist)
    }

    suspend fun updatePlaylist(playlist: Playlist) {
        playlistDao.updatePlaylist(playlist)
    }

    suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.deletePlaylistById(playlistId)
    }

    suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long) {
        playlistDao.addTrackToPlaylist(playlistId, trackId)
    }

    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) {
        playlistDao.removeTrackFromPlaylist(playlistId, trackId)
    }

    suspend fun clearPlaylist(playlistId: Long) {
        playlistDao.clearPlaylist(playlistId)
    }

    suspend fun getPlaylistTrackCount(playlistId: Long): Int =
        playlistDao.getPlaylistTrackCount(playlistId)
}