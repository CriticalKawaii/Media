package com.kiryusha.media

import android.app.Application
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kiryusha.media.database.AppDatabase

class MediaApplication : Application() {
    companion object {
        lateinit var database: AppDatabase
            private set

        lateinit var instance: MediaApplication
            private set

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create new user_favorites table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS user_favorites (
                        userId INTEGER NOT NULL,
                        trackId INTEGER NOT NULL,
                        added_at INTEGER NOT NULL,
                        PRIMARY KEY(userId, trackId),
                        FOREIGN KEY(userId) REFERENCES user(uid) ON DELETE CASCADE,
                        FOREIGN KEY(trackId) REFERENCES tracks(trackId) ON DELETE CASCADE
                    )
                """.trimIndent())

                // Create a temporary table without is_favorite column
                db.execSQL("""
                    CREATE TABLE tracks_new (
                        trackId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        artist TEXT NOT NULL,
                        album TEXT NOT NULL,
                        duration_ms INTEGER NOT NULL,
                        file_path TEXT NOT NULL,
                        album_art_uri TEXT,
                        date_added INTEGER NOT NULL,
                        play_count INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                // Copy data from old table to new table (excluding is_favorite)
                db.execSQL("""
                    INSERT INTO tracks_new (trackId, title, artist, album, duration_ms, file_path, album_art_uri, date_added, play_count)
                    SELECT trackId, title, artist, album, duration_ms, file_path, album_art_uri, date_added, play_count
                    FROM tracks
                """.trimIndent())

                // Drop old table
                db.execSQL("DROP TABLE tracks")

                // Rename new table to tracks
                db.execSQL("ALTER TABLE tracks_new RENAME TO tracks")

                // Recreate the index on file_path
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_tracks_file_path ON tracks(file_path)")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "media-database"
        )
            .addMigrations(MIGRATION_4_5)
            .fallbackToDestructiveMigration()
            .build()
    }
}