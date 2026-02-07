package com.blankdev.crossfade.api

import com.google.gson.annotations.SerializedName

data class ITunesResponse(
    @SerializedName("resultCount") val resultCount: Int,
    @SerializedName("results") val results: List<ITunesResult>
)

data class ITunesResult(
    @SerializedName("wrapperType") val wrapperType: String?,
    @SerializedName("kind") val kind: String?,
    @SerializedName("artistId") val artistId: Long?,
    @SerializedName("collectionId") val collectionId: Long?,
    @SerializedName("trackId") val trackId: Long?,
    @SerializedName("artistName") val artistName: String?,
    @SerializedName("collectionName") val collectionName: String?,
    @SerializedName("trackName") val trackName: String?,
    @SerializedName("artworkUrl60") val artworkUrl60: String?,
    @SerializedName("artworkUrl100") val artworkUrl100: String?,
    @SerializedName("artworkUrl160") val artworkUrl160: String?,
    @SerializedName("artworkUrl600") val artworkUrl600: String?,
    @SerializedName("trackViewUrl") val trackViewUrl: String?,
    @SerializedName("collectionViewUrl") val collectionViewUrl: String?
)
