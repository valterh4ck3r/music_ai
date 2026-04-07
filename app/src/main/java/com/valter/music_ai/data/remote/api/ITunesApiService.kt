package com.valter.music_ai.data.remote.api

import com.valter.music_ai.data.remote.dto.ITunesSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ITunesApiService {

    @GET("search")
    suspend fun searchSongs(
        @Query("term") term: String,
        @Query("media") media: String = "music",
        @Query("entity") entity: String = "song",
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("country") country: String = "US"
    ): ITunesSearchResponse

    companion object {
        const val BASE_URL = "https://itunes.apple.com/"
    }
}
