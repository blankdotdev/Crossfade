package com.blankdev.crossfade.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

data class OEmbedResponse(
    @SerializedName("title") val title: String?,
    @SerializedName("author_name") val authorName: String?,
    @SerializedName("provider_name") val providerName: String?,
    @SerializedName("thumbnail_url") val thumbnailUrl: String?
)

interface OEmbedApi {
    @GET
    suspend fun getOEmbed(@Url url: String): Response<OEmbedResponse>
}
