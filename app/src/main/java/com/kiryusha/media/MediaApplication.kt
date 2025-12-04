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

                db.execSQL("""
                    INSERT INTO tracks_new (trackId, title, artist, album, duration_ms, file_path, album_art_uri, date_added, play_count)
                    SELECT trackId, title, artist, album, duration_ms, file_path, album_art_uri, date_added, play_count
                    FROM tracks
                """.trimIndent())

                db.execSQL("DROP TABLE tracks")

                db.execSQL("ALTER TABLE tracks_new RENAME TO tracks")

                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_tracks_file_path ON tracks(file_path)")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS user_tracks (
                        userId INTEGER NOT NULL,
                        trackId INTEGER NOT NULL,
                        added_at INTEGER NOT NULL,
                        PRIMARY KEY(userId, trackId),
                        FOREIGN KEY(userId) REFERENCES user(uid) ON DELETE CASCADE,
                        FOREIGN KEY(trackId) REFERENCES tracks(trackId) ON DELETE CASCADE
                    )
                """.trimIndent())

                db.execSQL("CREATE INDEX IF NOT EXISTS index_user_tracks_userId ON user_tracks(userId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_user_tracks_trackId ON user_tracks(trackId)")

                db.execSQL("""
                    INSERT INTO user_tracks (userId, trackId, added_at)
                    SELECT u.uid, t.trackId, t.date_added
                    FROM user u
                    CROSS JOIN tracks t
                """.trimIndent())
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tracks ADD COLUMN lyrics TEXT DEFAULT NULL")
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
            .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
            .fallbackToDestructiveMigration()
            .build()
    }
}