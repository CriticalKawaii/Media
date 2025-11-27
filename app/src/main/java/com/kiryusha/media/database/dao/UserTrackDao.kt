package com.kiryusha.media.database.dao

import androidx.room.*
import com.kiryusha.media.database.entities.Track
import com.kiryusha.media.database.entities.UserTrack
import kotlinx.coroutines.flow.Flow

@Dao
interface UserTrackDao {

    @Query("""
        SELECT t.* FROM tracks t
        INNER JOIN user_tracks ut ON t.trackId = ut.trackId
        WHERE ut.userId = :userId
        ORDER BY ut.added_at DESC
    """)
    fun getUserTracks(userId: Int): Flow<List<Track>>

    @Query("""
        SELECT t.* FROM tracks t
        INNER JOIN user_tracks ut ON t.trackId = ut.trackId
        WHERE ut.userId = :userId AND t.album = :albumName
        ORDER BY t.title
    """)
    fun getUserTracksByAlbum(userId: Int, albumName: String): Flow<List<Track>>

    @Query("""
        SELECT t.* FROM tracks t
        INNER JOIN user_tracks ut ON t.trackId = ut.trackId
        WHERE ut.userId = :userId AND t.artist = :artistName
        ORDER BY t.album, t.title
    """)
    fun getUserTracksByArtist(userId: Int, artistName: String): Flow<List<Track>>

    @Query("""
        SELECT t.* FROM tracks t
        INNER JOIN user_tracks ut ON t.trackId = ut.trackId
        WHERE ut.userId = :userId
        AND (t.title LIKE '%' || :query || '%'
        OR t.artist LIKE '%' || :query || '%'
        OR t.album LIKE '%' || :query || '%')
        ORDER BY t.play_count DESC
    """)
    fun searchUserTracks(userId: Int, query: String): Flow<List<Track>>

    @Query("""
        SELECT DISTINCT t.album, t.artist, t.album_art_uri
        FROM tracks t
        INNER JOIN user_tracks ut ON t.trackId = ut.trackId
        WHERE ut.userId = :userId
        ORDER BY t.album
    """)
    suspend fun getUserAlbums(userId: Int): List<com.kiryusha.media.database.dao.AlbumInfo>

    @Query("""
        SELECT DISTINCT t.artist
        FROM tracks t
        INNER JOIN user_tracks ut ON t.trackId = ut.trackId
        WHERE ut.userId = :userId
        ORDER BY t.artist
    """)
    suspend fun getUserArtists(userId: Int): List<String>

    @Query("""
        SELECT COUNT(*) FROM user_tracks
        WHERE userId = :userId
    """)
    suspend fun getUserTrackCount(userId: Int): Int

    @Query("SELECT * FROM user_tracks WHERE userId = :userId AND trackId = :trackId")
    suspend fun getUserTrack(userId: Int, trackId: Long): UserTrack?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addUserTrack(userTrack: UserTrack)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addUserTracks(userTracks: List<UserTrack>)

    @Query("DELETE FROM user_tracks WHERE userId = :userId AND trackId = :trackId")
    suspend fun removeUserTrack(userId: Int, trackId: Long)

    @Query("DELETE FROM user_tracks WHERE userId = :userId")
    suspend fun removeAllUserTracks(userId: Int)

    @Query("SELECT EXISTS(SELECT 1 FROM user_tracks WHERE userId = :userId AND trackId = :trackId)")
    suspend fun isTrackInUserLibrary(userId: Int, trackId: Long): Boolean
}
