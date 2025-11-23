package com.kiryusha.media.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "playback_history",
    foreignKeys = [
        ForeignKey(
            entity = Track::class,
            parentColumns = ["trackId"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PlaybackHistory(
    @PrimaryKey(autoGenerate = true) val historyId: Long = 0,
    @ColumnInfo(name = "trackId") val trackId: Long,
    @ColumnInfo(name = "played_at") val playedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "user_id") val userId: Int
)
