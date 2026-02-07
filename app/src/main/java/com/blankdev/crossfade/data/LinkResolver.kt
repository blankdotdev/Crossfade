package com.blankdev.crossfade.data

import com.blankdev.crossfade.api.*
import com.blankdev.crossfade.utils.PlatformRegistry
import com.blankdev.crossfade.utils.SettingsManager
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import org.jsoup.Jsoup

class LinkResolver(private val historyDao: HistoryDao) {

    private val okHttpClient by lazy {
        okhttp3.OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "Crossfade/1.0.2 (Android; Mobile)")
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    private val odesliApi: OdesliApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.song.link/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OdesliApi::class.java)
    }

    private val oEmbedApi: OEmbedApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://open.spotify.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OEmbedApi::class.java)
    }

    private val itunesApi: ITunesApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://itunes.apple.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ITunesApi::class.java)
    }

    private val fallbackResolver by lazy {
        FallbackResolver(historyDao, oEmbedApi)
    }

    suspend fun resolveLink(url: String): ResolveResult {
        return withContext(Dispatchers.IO) {
            // 0. Check Database Cache First
            val existingItem = historyDao.getHistoryItemByUrl(url)
            if (existingItem != null && existingItem.isResolved) {
                if (!existingItem.linksJson.isNullOrBlank() && existingItem.linksJson != "{}") {
                    try {
                        val linksType = object : com.google.gson.reflect.TypeToken<Map<String, PlatformLink>>() {}.type
                        val linksByPlatform: Map<String, PlatformLink> = Gson().fromJson(existingItem.linksJson, linksType)
                        
                        val response = OdesliResponse(
                            entityUniqueId = null,
                            userCountry = null,
                            pageUrl = existingItem.pageUrl,
                            entitiesByUniqueId = null,
                            linksByPlatform = linksByPlatform
                        )
                        return@withContext ResolveResult.Success(response, existingItem)
                    } catch (e: Exception) {
                        android.util.Log.w("LinkResolver", "Failed to parse cached links: ${e.message}")
                    }
                }
            }

            // 1. Pre-process SoundCloud short links
            var effectiveUrl = url
            if (url.contains("on.soundcloud.com")) {
                 try {
                     val response = Jsoup.connect(url)
                         .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                         .followRedirects(true)
                         .execute()
                     effectiveUrl = response.url().toString()
                     
                     val resolvedExisting = historyDao.getHistoryItemByUrl(effectiveUrl)
                     if (resolvedExisting != null && resolvedExisting.isResolved) {
                         return@withContext resolveLink(effectiveUrl) 
                     }
                 } catch (e: Exception) {
                     android.util.Log.e("LinkResolver", "SoundCloud redirect failed: ${e.message}")
                 }
            }

            // 2. Try Odesli
            try {
                val response = odesliApi.resolveLink(effectiveUrl)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val entityUniqueId = body.entityUniqueId ?: body.entitiesByUniqueId?.keys?.firstOrNull()
                    
                    if (entityUniqueId != null) {
                        val entity = body.entitiesByUniqueId?.get(entityUniqueId)
                        val title = entity?.title
                        val artist = entity?.artistName
                        val thumbnail = entity?.thumbnailUrl
                        
                        if (!title.isNullOrBlank()) {
                            val linksJson = Gson().toJson(body.linksByPlatform)
                            val existingForUrl = historyDao.getHistoryItemByUrl(url)
                            val newId = existingForUrl?.id ?: 0
                            
                            val isPodcast = entity?.type == "podcast" || entity?.type == "podcastShow"
                            val isPodcastEpisode = entity?.type == "podcastEpisode"
                            
                            val historyItem = HistoryItem(
                                id = newId,
                                originalUrl = url,
                                songTitle = title,
                                artistName = artist,
                                thumbnailUrl = thumbnail,
                                originalImageUrl = thumbnail,
                                pageUrl = body.pageUrl,
                                linksJson = linksJson,
                                isResolved = true,
                                isPodcast = isPodcast,
                                isPodcastEpisode = isPodcastEpisode
                            )
                            val savedId = historyDao.insert(historyItem)
                            val historyItemWithId = historyItem.copy(id = savedId)

                            return@withContext ResolveResult.Success(body, historyItemWithId)
                        }
                    }
                }
            } catch (e: Exception) {}
            
            // 3. Fallback
            return@withContext fallbackResolver.tryFallback(url)
        }
    }

    suspend fun searchITunes(query: String, type: String): List<ITunesResult> {
        return withContext(Dispatchers.IO) {
            try {
                val entity = when (type.lowercase()) {
                    "song" -> "song"
                    "album" -> "album"
                    "pod" -> "podcast"
                    "episode" -> "podcastEpisode"
                    else -> "song"
                }
                val response = itunesApi.search(query, entity)
                if (response.isSuccessful && response.body() != null) {
                    response.body()!!.results
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun resolveManual(
        originalItem: HistoryItem,
        selectedUrl: String,
        fallbackTitle: String? = null,
        fallbackArtist: String? = null,
        fallbackThumbnail: String? = null
    ): ResolveResult {
        return withContext(Dispatchers.IO) {
            val itemToUpdate = if (originalItem.id == 0L) {
                historyDao.getHistoryItemByUrl(originalItem.originalUrl) ?: originalItem
            } else {
                originalItem
            }

            var lastError = "Failed to resolve the selected item."
            
            for (attempt in 1..2) {
                try {
                    val response = odesliApi.resolveLink(selectedUrl)
                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        val entityUniqueId = body.entityUniqueId ?: body.entitiesByUniqueId?.keys?.firstOrNull()
                        
                        val entity = body.entitiesByUniqueId?.get(entityUniqueId ?: "")
                        val title = entity?.title ?: "Unknown Title"
                        val artist = entity?.artistName
                        val thumbnail = entity?.thumbnailUrl
                        val isAlbum = entity?.type == "album"
                        val isPodcast = entity?.type == "podcast" || entity?.type == "podcastShow"
                        val isPodcastEpisode = entity?.type == "podcastEpisode"
                        
                        val linksJson = Gson().toJson(body.linksByPlatform)
                        
                        val historyItem = itemToUpdate.copy(
                            songTitle = title,
                            artistName = artist,
                            isAlbum = isAlbum,
                            thumbnailUrl = thumbnail,
                            originalImageUrl = thumbnail,
                            pageUrl = body.pageUrl,
                            linksJson = linksJson,
                            isResolved = true,
                            isPodcast = isPodcast,
                            isPodcastEpisode = isPodcastEpisode
                        )
                        historyDao.update(historyItem)

                        return@withContext ResolveResult.Success(body, historyItem)
                    } else if (response.code() == 405 || response.code() == 400) {
                        // Handle Unsupported URL or other client errors by falling back if metadata is provided
                        if (fallbackTitle != null) {
                            val isPodcast = originalItem.originalUrl.contains("podcasts.apple.com") || com.blankdev.crossfade.utils.PlatformRegistry.isPodcastUrl(originalItem.originalUrl)
                            val isPodcastEpisode = isPodcast && originalItem.originalUrl.contains("?i=")
                            
                            val linksMap = mutableMapOf<String, com.blankdev.crossfade.api.PlatformLink>()
                            val platformId = com.blankdev.crossfade.utils.PlatformRegistry.getPodcastPlatformFromUrl(selectedUrl)
                                ?: com.blankdev.crossfade.utils.PlatformRegistry.getPlatformFromUrl(selectedUrl)
                            
                            val odesliKey = com.blankdev.crossfade.utils.PlatformRegistry.getPlatformByInternalId(platformId ?: "")?.odesliKey
                            if (odesliKey != null) {
                                linksMap[odesliKey] = com.blankdev.crossfade.api.PlatformLink(entityUniqueId = null, url = selectedUrl, nativeAppUriMobile = null, nativeAppUriDesktop = null)
                            }

                            val historyItem = itemToUpdate.copy(
                                songTitle = fallbackTitle,
                                artistName = fallbackArtist,
                                thumbnailUrl = fallbackThumbnail,
                                originalImageUrl = fallbackThumbnail,
                                pageUrl = selectedUrl,
                                linksJson = Gson().toJson(linksMap),
                                isResolved = true,
                                isPodcast = isPodcast,
                                isPodcastEpisode = isPodcastEpisode
                            )
                            historyDao.update(historyItem)
                            
                            val dummyResponse = OdesliResponse(
                                entityUniqueId = null,
                                userCountry = null,
                                pageUrl = selectedUrl,
                                entitiesByUniqueId = null,
                                linksByPlatform = emptyMap()
                            )
                            return@withContext ResolveResult.Success(dummyResponse, historyItem)
                        }
                        lastError = "Odesli error: ${response.code()} ${response.message()}"
                    } else {
                        lastError = "Odesli error: ${response.code()} ${response.message()}"
                    }
                } catch (e: Exception) {
                    lastError = "Network error: ${e.message}"
                }
                
                if (attempt == 1) {
                    kotlinx.coroutines.delay(1500)
                }
            }
            
            return@withContext ResolveResult.Error("$lastError. Please try again.")
        }
    }

    suspend fun saveUnresolvedLink(url: String) {
        val existingItem = historyDao.getHistoryItemByUrl(url)
        if (existingItem != null && existingItem.isResolved) return
        
        val newId = existingItem?.id ?: 0
        val historyItem = HistoryItem(
            id = newId,
            originalUrl = url,
            songTitle = null,
            artistName = null,
            thumbnailUrl = null,
            originalImageUrl = null,
            linksJson = null,
            isResolved = false
        )
        historyDao.insert(historyItem)
    }

    fun getTargetUrl(response: OdesliResponse, targetApp: String): String? {
        val links = response.linksByPlatform ?: return null
        
        val platformInfo = PlatformRegistry.getPlatformByInternalId(targetApp) ?: return response.pageUrl
        val platformKey = platformInfo.odesliKey ?: return response.pageUrl

        var link = links[platformKey]
        if (link == null && platformKey == "appleMusic") {
            link = links["itunes"]
        }
        
        return link?.nativeAppUriMobile ?: link?.url ?: response.pageUrl
    }

    fun getPodcastTargetUrl(response: OdesliResponse, targetApp: String): String? {
        val links = response.linksByPlatform ?: return null
        
        val platformKey = when (targetApp) {
            SettingsManager.TARGET_PODCAST_SPOTIFY -> "spotify"
            SettingsManager.TARGET_PODCAST_APPLE -> "appleMusic"
            else -> return response.pageUrl
        }

        var link = links[platformKey]
        if (link == null && platformKey == "appleMusic") {
            link = links["itunes"]
        }
        return link?.url ?: response.pageUrl
    }
}

sealed class ResolveResult {
    data class Success(val data: OdesliResponse, val historyItem: HistoryItem) : ResolveResult()
    data class Fallback(val historyItem: HistoryItem, val searchQuery: String) : ResolveResult()
    data class Error(val message: String) : ResolveResult()
}
