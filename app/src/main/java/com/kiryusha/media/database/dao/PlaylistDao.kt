package com.kiryusha.media.database.dao

import androidx.room.*
import com.kiryusha.media.database.entities.Playlist
import com.kiryusha.media.database.entities.PlaylistTrackCrossRef
import com.kiryusha.media.database.entities.PlaylistWithTracks
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Query("SELECT * FROM playlists WHERE user_id = :userId ORDER BY created_at DESC")
    fun getUserPlaylists(userId: Int): Flow<List<Playlist>>

    @Query("SELECT * FROM playlists WHERE playlistId = :playlistId")
    suspend fun getPlaylistById(playlistId: Long): Playlist?

    @Transaction
    @Query("SELECT * FROM playlists WHERE playlistId = :playlistId")
    suspend fun getPlaylistWithTracks(playlistId: Long): PlaylistWithTracks?

    @Transaction
    @Query("SELECT * FROM playlists WHERE user_id = :userId ORDER BY created_at DESC")
    fun getUserPlaylistsWithTracks(userId: Int): Flow<List<PlaylistWithTracks>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Update
    suspend fun updatePlaylist(playlist: Playlist)

    @Delete
    suspend fun deletePlaylist(playlist: Playlist)

    @Query("DELETE FROM playlists WHERE playlistId = :playlistId")
    suspend fun deletePlaylistById(playlistId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistTrack(crossRef: PlaylistTrackCrossRef)

    @Delete
    suspend fun deletePlaylistTrack(crossRef: PlaylistTrackCrossRef)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun clearPlaylist(playlistId: Long)

    @Query("""
        SELECT MAX(position) FROM playlist_tracks 
        WHERE playlistId = :playlistId
    """)
    suspend fun getMaxPosition(playlistId: Long): Int?

    @Query("""
        SELECT COUNT(*) FROM playlist_tracks 
        WHERE playlistId = :playlistId
    """)
    suspend fun getPlaylistTrackCount(playlistId: Long): Int

    @Transaction
    suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long) {
        val maxPosition = getMaxPosition(playlistId) ?: -1
        val crossRef = PlaylistTrackCrossRef(
            playlistId = playlistId,
            trackId = trackId,
            position = maxPosition + 1
        )
        insertPlaylistTrack(crossRef)
    }
}
