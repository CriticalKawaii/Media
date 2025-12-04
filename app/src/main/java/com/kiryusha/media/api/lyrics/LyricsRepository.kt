package com.kiryusha.media.api.lyrics

import android.util.Log
import com.kiryusha.media.api.NetworkModule
import com.kiryusha.media.database.dao.TrackDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder

class LyricsRepository(private val trackDao: TrackDao) {

    private val apiService = NetworkModule.lyricsApiService

    suspend fun getLyrics(trackId: Long, artist: String, title: String): LyricsResult {
        return withContext(Dispatchers.IO) {
            try {
                val track = trackDao.getTrackById(trackId)
                if (track?.lyrics != null) {
                    Log.d("LyricsRepository", "Returning cached lyrics for: $title by $artist")
                    return@withContext LyricsResult.Success(track.lyrics)
                }

                Log.d("LyricsRepository", "Fetching lyrics from API for: $title by $artist")

                val cleanArtist = cleanString(artist)
                val cleanTitle = cleanString(title)

                val response = apiService.getLyrics(cleanArtist, cleanTitle)

                if (response.isSuccessful) {
                    val lyrics = response.body()?.lyrics
                    if (!lyrics.isNullOrBlank()) {
                        trackDao.updateLyrics(trackId, lyrics)
                        Log.d("LyricsRepository", "Successfully fetched and cached lyrics")
                        LyricsResult.Success(lyrics)
                    } else {
                        Log.w("LyricsRepository", "Lyrics not found for: $title by $artist")
                        LyricsResult.NotFound
                    }
                } else {
                    Log.e("LyricsRepository", "API error: ${response.code()} - ${response.message()}")
                    LyricsResult.Error("Failed to fetch lyrics: ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("LyricsRepository", "Exception fetching lyrics", e)
                LyricsResult.Error("Network error: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    private fun cleanString(input: String): String {
        var cleaned = input.replace(Regex("\\([^)]*\\)"), "")
        cleaned = cleaned.replace(Regex("\\[[^]]*]"), "")
        cleaned = cleaned.replace(Regex("[^a-zA-Z0-9\\s'-]"), "")
        return URLEncoder.encode(cleaned.trim(), "UTF-8")
    }

    suspend fun clearLyrics(trackId: Long) {
        withContext(Dispatchers.IO) {
            trackDao.updateLyrics(trackId, null)
        }
    }
}
