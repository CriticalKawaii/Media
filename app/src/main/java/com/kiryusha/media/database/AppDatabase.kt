package com.kiryusha.media.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kiryusha.media.database.daos.UserDao
import com.kiryusha.media.database.entities.User

@Database(entities = [User::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}