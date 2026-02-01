package com.blankdev.crossfade.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ITunesApi {
    @GET("search")
    suspend fun search(
        @Query("term") term: String,
        @Query("entity") entity: String, // song, album, podcast, podcastEpisode
        @Query("limit") limit: Int = 20,
        @Query("country") country: String? = null
    ): Response<ITunesResponse>
}
