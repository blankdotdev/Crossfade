package com.blankdev.crossfade.api

import com.google.gson.annotations.SerializedName

data class OdesliResponse(
    @SerializedName("entityUniqueId") val entityUniqueId: String?,
    @SerializedName("userCountry") val userCountry: String?,
    @SerializedName("pageUrl") val pageUrl: String?,
    @SerializedName("entitiesByUniqueId") val entitiesByUniqueId: Map<String, Entity>?,
    @SerializedName("linksByPlatform") val linksByPlatform: Map<String, PlatformLink>?
)

data class Entity(
    @SerializedName("id") val id: String?,
    @SerializedName("type") val type: String?, // song, album
    @SerializedName("title") val title: String?,
    @SerializedName("artistName") val artistName: String?,
    @SerializedName("thumbnailUrl") val thumbnailUrl: String?,
    @SerializedName("thumbnailWidth") val thumbnailWidth: Int?,
    @SerializedName("thumbnailHeight") val thumbnailHeight: Int?,
    @SerializedName("apiProvider") val apiProvider: String?,
    @SerializedName("platforms") val platforms: List<String>?
)

data class PlatformLink(
    @SerializedName("entityUniqueId") val entityUniqueId: String?,
    @SerializedName("url") val url: String?,
    @SerializedName("nativeAppUriMobile") val nativeAppUriMobile: String?,
    @SerializedName("nativeAppUriDesktop") val nativeAppUriDesktop: String?
)
