package com.kiryusha.media.database.dao

import androidx.room.*
import com.kiryusha.media.database.entities.Track
import com.kiryusha.media.database.entities.UserFavorite
import kotlinx.coroutines.flow.Flow

@Dao
interface UserFavoriteDao {

    @Query("""
        SELECT tracks.* FROM tracks
        INNER JOIN user_favorites ON tracks.trackId = user_favorites.trackId
        WHERE user_favorites.userId = :userId
        ORDER BY user_favorites.added_at DESC
    """)
    fun getFavoriteTracksForUser(userId: Int): Flow<List<Track>>

    @Query("SELECT * FROM user_favorites WHERE userId = :userId AND trackId = :trackId")
    suspend fun getFavorite(userId: Int, trackId: Long): UserFavorite?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(userFavorite: UserFavorite)

    @Query("DELETE FROM user_favorites WHERE userId = :userId AND trackId = :trackId")
    suspend fun removeFavorite(userId: Int, trackId: Long)

    @Query("SELECT COUNT(*) FROM user_favorites WHERE userId = :userId AND trackId = :trackId")
    suspend fun isFavorite(userId: Int, trackId: Long): Int

    @Query("SELECT COUNT(*) FROM user_favorites WHERE userId = :userId")
    suspend fun getFavoriteCount(userId: Int): Int
}
