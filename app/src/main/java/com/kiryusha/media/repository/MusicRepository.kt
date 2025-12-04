package com.kiryusha.media.repository

import com.kiryusha.media.database.dao.PlaybackHistoryDao
import com.kiryusha.media.database.dao.TrackDao
import com.kiryusha.media.database.dao.UserFavoriteDao
import com.kiryusha.media.database.dao.UserTrackDao
import com.kiryusha.media.database.entities.Album
import com.kiryusha.media.database.entities.PlaybackHistory
import com.kiryusha.media.database.entities.Track
import com.kiryusha.media.database.entities.UserFavorite
import com.kiryusha.media.database.entities.UserTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class MusicRepository(
    private val trackDao: TrackDao,
    private val historyDao: PlaybackHistoryDao,
    private val userFavoriteDao: UserFavoriteDao,
    private val userTrackDao: UserTrackDao
) {

    fun getUserTracks(userId: Int): Flow<List<Track>> = userTrackDao.getUserTracks(userId)

    fun getAllTracks(): Flow<List<Track>> = trackDao.getAllTracks()

    suspend fun getTrackById(trackId: Long): Track? = trackDao.getTrackById(trackId)

    fun getTracksByAlbum(userId: Int, albumName: String): Flow<List<Track>> =
        userTrackDao.getUserTracksByAlbum(userId, albumName)

    fun getTracksByArtist(userId: Int, artistName: String): Flow<List<Track>> =
        userTrackDao.getUserTracksByArtist(userId, artistName)

    fun getFavoriteTracks(userId: Int): Flow<List<Track>> = userFavoriteDao.getFavoriteTracksForUser(userId)

    fun searchTracks(userId: Int, query: String): Flow<List<Track>> = userTrackDao.searchUserTracks(userId, query)

    suspend fun getAllAlbums(userId: Int): List<Album> = withContext(Dispatchers.IO) {
        val albumInfos = userTrackDao.getUserAlbums(userId)
        albumInfos.map { info ->
            Album(
                name = info.album,
                artist = info.artist,
                coverUri = info.album_art_uri,
                tracks = emptyList()
            )
        }
    }

    suspend fun getAlbumWithTracks(userId: Int, albumName: String): Album? = withContext(Dispatchers.IO) {
        val tracksList = userTrackDao.getUserTracksByAlbum(userId, albumName).first()

        if (tracksList.isEmpty()) return@withContext null

        Album(
            name = albumName,
            artist = tracksList.first().artist,
            coverUri = tracksList.first().albumArtUri,
            tracks = tracksList
        )
    }

    suspend fun toggleFavorite(userId: Int, trackId: Long, isFavorite: Boolean) {
        if (isFavorite) {
            userFavoriteDao.addFavorite(UserFavorite(userId, trackId))
        } else {
            userFavoriteDao.removeFavorite(userId, trackId)
        }
    }

    suspend fun isFavorite(userId: Int, trackId: Long): Boolean {
        return userFavoriteDao.isFavorite(userId, trackId) > 0
    }

    suspend fun importTracks(userId: Int, tracks: List<Track>) {
        val userTracks = tracks.mapNotNull { track ->
            val insertedId = trackDao.insertTrack(track)

            val existingTrack = trackDao.getTrackByFilePath(track.filePath)

            existingTrack?.let {
                UserTrack(userId = userId, trackId = it.trackId)
            }
        }

        userTrackDao.addUserTracks(userTracks)
    }

    suspend fun getTrackCount(userId: Int): Int = userTrackDao.getUserTrackCount(userId)

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
            totalTracks = userTrackDao.getUserTrackCount(userId),
            totalPlays = historyDao.getTotalPlays(userId),
            uniqueTracksPlayed = historyDao.getUniqueTracksPlayed(userId)
        )
    }

    suspend fun deleteTrack(userId: Int, track: Track) {
        userTrackDao.removeUserTrack(userId, track.trackId)
    }

    suspend fun deleteTracks(userId: Int, tracks: List<Track>) {
        tracks.forEach { track ->
            userTrackDao.removeUserTrack(userId, track.trackId)
        }
    }
}

