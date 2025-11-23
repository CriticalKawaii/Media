package com.kiryusha.media.database.dao

import androidx.room.*
import com.kiryusha.media.database.entities.PlaybackHistory
import com.kiryusha.media.database.entities.Track
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackHistoryDao {

    @Query("""
        SELECT tracks.* FROM tracks
        INNER JOIN playback_history ON tracks.trackId = playback_history.trackId
        WHERE playback_history.user_id = :userId
        ORDER BY playback_history.played_at DESC
        LIMIT :limit
    """)
    fun getRecentlyPlayed(userId: Int, limit: Int = 20): Flow<List<Track>>

    @Query("""
        SELECT tracks.* FROM tracks
        INNER JOIN playback_history ON tracks.trackId = playback_history.trackId
        WHERE playback_history.user_id = :userId
        GROUP BY tracks.trackId
        ORDER BY COUNT(playback_history.historyId) DESC
        LIMIT :limit
    """)
    fun getMostPlayed(userId: Int, limit: Int = 20): Flow<List<Track>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: PlaybackHistory)

    @Query("DELETE FROM playback_history WHERE user_id = :userId")
    suspend fun clearHistory(userId: Int)

    @Query("""
        DELETE FROM playback_history 
        WHERE played_at < :timestamp
    """)
    suspend fun deleteOldHistory(timestamp: Long)

    @Query("""
        SELECT COUNT(DISTINCT trackId) FROM playback_history 
        WHERE user_id = :userId
    """)
    suspend fun getUniqueTracksPlayed(userId: Int): Int

    @Query("""
        SELECT COUNT(*) FROM playback_history 
        WHERE user_id = :userId
    """)
    suspend fun getTotalPlays(userId: Int): Int
}