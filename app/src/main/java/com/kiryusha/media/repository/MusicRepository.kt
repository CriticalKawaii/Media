package com.kiryusha.media.repository

import com.kiryusha.media.database.dao.PlaybackHistoryDao
import com.kiryusha.media.database.dao.TrackDao
import com.kiryusha.media.database.entities.Album
import com.kiryusha.media.database.entities.PlaybackHistory
import com.kiryusha.media.database.entities.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class MusicRepository(
    private val trackDao: TrackDao,
    private val historyDao: PlaybackHistoryDao
) {

    fun getAllTracks(): Flow<List<Track>> = trackDao.getAllTracks()

    suspend fun getTrackById(trackId: Long): Track? = trackDao.getTrackById(trackId)

    fun getTracksByAlbum(albumName: String): Flow<List<Track>> =
        trackDao.getTracksByAlbum(albumName)

    fun getTracksByArtist(artistName: String): Flow<List<Track>> =
        trackDao.getTracksByArtist(artistName)

    fun getFavoriteTracks(): Flow<List<Track>> = trackDao.getFavoriteTracks()

    fun searchTracks(query: String): Flow<List<Track>> = trackDao.searchTracks(query)

    suspend fun getAllAlbums(): List<Album> = withContext(Dispatchers.IO) {
        val albumInfos = trackDao.getAllAlbums()
        albumInfos.map { info ->
            Album(
                name = info.album,
                artist = info.artist,
                coverUri = info.album_art_uri,
                tracks = emptyList()
            )
        }
    }

    suspend fun getAlbumWithTracks(albumName: String): Album? = withContext(Dispatchers.IO) {
        val tracks = trackDao.getTracksByAlbum(albumName).first()

        if (tracks.isEmpty()) return@withContext null

        Album(
            name = albumName,
            artist = tracks.first().artist,
            coverUri = tracks.first().albumArtUri,
            tracks = tracks
        )
    }

    suspend fun toggleFavorite(trackId: Long, isFavorite: Boolean) {
        trackDao.updateFavoriteStatus(trackId, isFavorite)
    }

    suspend fun importTracks(tracks: List<Track>) {
        trackDao.insertTracks(tracks)
    }

    suspend fun getTrackCount(): Int = trackDao.getTrackCount()

    suspend fun recordPlay(trackId: Long, userId: Int) {
        trackDao.incrementPlayCount(trackId)
        historyDao.insertHistory(
            PlaybackHistory(
                trackId = trackId,
                userId = userId
            )
        )
    }

    fun getRecentlyPlayed(userId: Int): Flow<List<Track>> =
        historyDao.getRecentlyPlayed(userId)

    fun getMostPlayed(userId: Int): Flow<List<Track>> =
        historyDao.getMostPlayed(userId)

    suspend fun getUserStats(userId: Int): UserStats {
        return UserStats(
            totalTracks = trackDao.getTrackCount(),
            totalPlays = historyDao.getTotalPlays(userId),
            uniqueTracksPlayed = historyDao.getUniqueTracksPlayed(userId)
        )
    }
}

