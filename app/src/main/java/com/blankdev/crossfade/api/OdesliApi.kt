package com.blankdev.crossfade.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface OdesliApi {
    // https://api.song.link/v1-alpha.1/links?url={url}&userCountry={country}
    @GET("v1-alpha.1/links")
    suspend fun resolveLink(
        @Query("url") url: String,
        @Query("userCountry") userCountry: String? = null,
        @Query("songIfSingle") songIfSingle: Boolean? = null
    ): Response<OdesliResponse>
}
