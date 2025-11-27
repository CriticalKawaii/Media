package com.kiryusha.media.api

import com.kiryusha.media.api.lyrics.LyricsApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Network module for creating Retrofit instances
 */
object NetworkModule {

    private const val LYRICS_BASE_URL = "https://api.lyrics.ovh/"
    private const val TIMEOUT_SECONDS = 30L

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    private val lyricsRetrofit = Retrofit.Builder()
        .baseUrl(LYRICS_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val lyricsApiService: LyricsApiService by lazy {
        lyricsRetrofit.create(LyricsApiService::class.java)
    }
}
