package com.kiryusha.media.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kiryusha.media.database.dao.PlaybackHistoryDao
import com.kiryusha.media.database.dao.PlaylistDao
import com.kiryusha.media.database.dao.TrackDao
import com.kiryusha.media.database.dao.UserDao
import com.kiryusha.media.database.entities.*

@Database(
    entities = [
        User::class,
        Track::class,
        Playlist::class,
        PlaylistTrackCrossRef::class,
        PlaybackHistory::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun trackDao(): TrackDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playbackHistoryDao(): PlaybackHistoryDao
}