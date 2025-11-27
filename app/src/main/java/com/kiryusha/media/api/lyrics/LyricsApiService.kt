package com.kiryusha.media.api.lyrics

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Retrofit interface for Lyrics.ovh API
 * Documentation: https://lyricsovh.docs.apiary.io/
 */
interface LyricsApiService {

    /**
     * Fetch lyrics for a given artist and song title
     * @param artist The artist name
     * @param title The song title
     * @return Response containing lyrics data
     */
    @GET("v1/{artist}/{title}")
    suspend fun getLyrics(
        @Path("artist") artist: String,
        @Path("title") title: String
    ): Response<LyricsOvhResponse>
}
