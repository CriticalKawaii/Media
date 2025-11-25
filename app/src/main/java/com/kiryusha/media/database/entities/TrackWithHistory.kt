package com.kiryusha.media.database.entities

import androidx.room.Embedded
import androidx.room.Relation

data class TrackWithHistory(
    @Embedded val track: Track,
    @Relation(
        parentColumn = "trackId",
        entityColumn = "trackId"
    )
    val history: List<PlaybackHistory>
)