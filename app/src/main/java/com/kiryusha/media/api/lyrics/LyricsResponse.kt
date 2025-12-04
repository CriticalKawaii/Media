package com.kiryusha.media.api.lyrics

import com.google.gson.annotations.SerializedName

data class LyricsOvhResponse(
    @SerializedName("lyrics")
    val lyrics: String?
)

sealed class LyricsResult {
    data class Success(val lyrics: String) : LyricsResult()
    data class Error(val message: String) : LyricsResult()
    object NotFound : LyricsResult()
    object Loading : LyricsResult()
}
