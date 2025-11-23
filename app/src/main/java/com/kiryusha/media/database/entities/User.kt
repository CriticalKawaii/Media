package com.kiryusha.media.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "user",
    indices = [Index(value = ["login"], unique = true)]
)
data class User(
    @PrimaryKey(autoGenerate = true) val uid: Int = 0,
    @ColumnInfo(name = "login") val login: String,
    @ColumnInfo(name = "user_name") val userName: String,
    @ColumnInfo(name = "password") val password: String,
)