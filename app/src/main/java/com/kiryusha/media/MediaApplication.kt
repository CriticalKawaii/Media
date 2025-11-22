package com.kiryusha.media

import android.app.Application
import androidx.room.Room
import com.kiryusha.media.database.AppDatabase

class MediaApplication : Application() {
    companion object {
        lateinit var database: AppDatabase
            private set
    }

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext,
            klass = AppDatabase::class.java,
            name = "media-database"
        ).build()
    }
}