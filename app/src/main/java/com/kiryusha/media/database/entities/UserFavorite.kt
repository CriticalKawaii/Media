package com.kiryusha.media.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "user_favorites",
    primaryKeys = ["userId", "trackId"],
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["uid"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Track::class,
            parentColumns = ["trackId"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class UserFavorite(
    val userId: Int,
    val trackId: Long,
    @ColumnInfo(name = "added_at")
    val addedAt: Long = System.currentTimeMillis()
)
