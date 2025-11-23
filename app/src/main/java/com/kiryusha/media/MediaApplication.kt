package com.kiryusha.media

import android.app.Application
import androidx.room.Room
import com.kiryusha.media.database.AppDatabase

class MediaApplication : Application() {
    companion object {
        lateinit var database: AppDatabase
            private set

        lateinit var instance: MediaApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "media-database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }
}