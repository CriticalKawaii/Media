package com.kiryusha.media.database.dao

import androidx.room.*
import com.kiryusha.media.database.entities.Track
import com.kiryusha.media.database.entities.TrackWithPlaylists
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {

    @Query("SELECT * FROM tracks ORDER BY date_added DESC")
    fun getAllTracks(): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE trackId = :trackId")
    suspend fun getTrackById(trackId: Long): Track?

    @Query("SELECT * FROM tracks WHERE album = :albumName ORDER BY title")
    fun getTracksByAlbum(albumName: String): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE artist = :artistName ORDER BY album, title")
    fun getTracksByArtist(artistName: String): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE is_favorite = 1 ORDER BY date_added DESC")
    fun getFavoriteTracks(): Flow<List<Track>>

    @Query("""
        SELECT * FROM tracks 
        WHERE title LIKE '%' || :query || '%' 
        OR artist LIKE '%' || :query || '%' 
        OR album LIKE '%' || :query || '%'
        ORDER BY play_count DESC
    """)
    fun searchTracks(query: String): Flow<List<Track>>

    @Query("SELECT DISTINCT album, artist, album_art_uri FROM tracks ORDER BY album")
    suspend fun getAllAlbums(): List<AlbumInfo>

    @Query("SELECT DISTINCT artist FROM tracks ORDER BY artist")
    suspend fun getAllArtists(): List<String>

    @Query("UPDATE tracks SET is_favorite = :isFavorite WHERE trackId = :trackId")
    suspend fun updateFavoriteStatus(trackId: Long, isFavorite: Boolean)

    @Query("UPDATE tracks SET play_count = play_count + 1 WHERE trackId = :trackId")
    suspend fun incrementPlayCount(trackId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTrack(track: Track): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTracks(tracks: List<Track>)

    @Update
    suspend fun updateTrack(track: Track)

    @Delete
    suspend fun deleteTrack(track: Track)

    @Query("DELETE FROM tracks")
    suspend fun deleteAllTracks()

    @Transaction
    @Query("SELECT * FROM tracks WHERE trackId = :trackId")
    suspend fun getTrackWithPlaylists(trackId: Long): TrackWithPlaylists?

    @Query("SELECT COUNT(*) FROM tracks")
    suspend fun getTrackCount(): Int
}

