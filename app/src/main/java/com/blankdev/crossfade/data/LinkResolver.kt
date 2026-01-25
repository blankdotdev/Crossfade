package com.blankdev.crossfade.data

import com.blankdev.crossfade.api.OEmbedApi
import com.blankdev.crossfade.api.OdesliApi
import com.blankdev.crossfade.api.OdesliResponse
import com.blankdev.crossfade.api.PlatformLink
import com.blankdev.crossfade.utils.SettingsManager
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import org.jsoup.Jsoup
import java.net.URLDecoder

class LinkResolver(private val historyDao: HistoryDao) {

    private val odesliApi: OdesliApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.song.link/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OdesliApi::class.java)
    }

    // Generic Retrofit client for dynamic OEmbed URLs
    private val oEmbedApi: OEmbedApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://open.spotify.com/") // Base URL ignored for @Url
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OEmbedApi::class.java)
    }

    suspend fun resolveLink(url: String): ResolveResult {
        return withContext(Dispatchers.IO) {
            // 0. Check Database Cache First
            val existingItem = historyDao.getHistoryItemByUrl(url)
            if (existingItem != null && existingItem.isResolved) {
                // If it's a "success" item (has links), reconstruct OdesliResponse
                if (!existingItem.linksJson.isNullOrBlank() && existingItem.linksJson != "{}") {
                    try {
                        val linksType = object : com.google.gson.reflect.TypeToken<Map<String, PlatformLink>>() {}.type
                        val linksByPlatform: Map<String, PlatformLink> = Gson().fromJson(existingItem.linksJson, linksType)
                        
                        val response = OdesliResponse(
                            entityUniqueId = null, // Not strictly needed for cached resolution
                            userCountry = null,
                            pageUrl = existingItem.pageUrl,
                            entitiesByUniqueId = null,
                            linksByPlatform = linksByPlatform
                        )
                        return@withContext ResolveResult.Success(response, existingItem)
                    } catch (e: Exception) {
                        // Fallback to network if JSON parsing fails
                    }
                } else if (existingItem.linksJson == "{}") {
                    // It's a fallback item (no direct links)
                    val searchQuery = if (existingItem.artistName != null) {
                        "${existingItem.songTitle} ${existingItem.artistName}"
                    } else {
                        existingItem.songTitle ?: url
                    }
                    return@withContext ResolveResult.Fallback(existingItem, searchQuery)
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
                     
                     // Optimization: Check cache again for the resolved SoundCloud URL
                     val resolvedExisting = historyDao.getHistoryItemByUrl(effectiveUrl)
                     if (resolvedExisting != null && resolvedExisting.isResolved) {
                         // Update the original short URL to point to this resolved item's data if needed
                         // For now, just return the resolved one
                         return@withContext resolveLink(effectiveUrl) 
                     }
                 } catch (e: Exception) {
                     // Fail silently and try with original URL
                 }
            }

            // 2. Try Odesli
            try {
                // Determine country - potentially from settings in future, default null (Odesli uses IP/US)
                val response = odesliApi.resolveLink(effectiveUrl, songIfSingle = true)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val entityUniqueId = body.entityUniqueId ?: return@withContext tryFallback(url, "No entity ID")
                    val entity = body.entitiesByUniqueId?.get(entityUniqueId)
                    
                    val title = entity?.title
                    val artist = entity?.artistName
                    val thumbnail = entity?.thumbnailUrl
                    
                    // If title is missing, Odesli failed to get meaningful data.
                    // Fallback to our internal scrapers (OpenGraph/Metadata).
                    if (title.isNullOrBlank()) {
                         return@withContext tryFallback(url, "Odesli returned empty title")
                    }
                    
                    // Save to History
                    val linksJson = Gson().toJson(body.linksByPlatform)
                    
                    // Check for existing item to avoid duplicates (using the original URL as key)
                    val existingForUrl = historyDao.getHistoryItemByUrl(url)
                    val newId = existingForUrl?.id ?: 0
                    
                    val historyItem = HistoryItem(
                        id = newId,
                        originalUrl = url,
                        songTitle = title,
                        artistName = artist,
                        thumbnailUrl = thumbnail,
                        originalImageUrl = thumbnail, // Store original for backup/restore
                        pageUrl = body.pageUrl,
                        linksJson = linksJson,
                        isResolved = true
                    )
                    historyDao.insert(historyItem)

                    return@withContext ResolveResult.Success(body, historyItem)
                }
            } catch (e: Exception) {
                // Ignore and try fallback
            }
            
            // 3. Fallback
            return@withContext tryFallback(url, "Odesli failed")
        }
    }

    private suspend fun tryFallback(originalUrl: String, @Suppress("UNUSED_PARAMETER") reason: String): ResolveResult {
        // Special handling for Shazam URLs
        if (originalUrl.contains("shazam.com")) {
            return tryShazamFallback(originalUrl)
        }
        
        // 1. Try OEmbed (Limited support: Spotify, Apple Music sometimes)
        try {
            val oEmbedUrl = getOEmbedUrl(originalUrl)
            if (oEmbedUrl != null) {
                val response = oEmbedApi.getOEmbed(oEmbedUrl)
                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    val title = data.title ?: "Unknown Title"
                    val artist = data.authorName
                    val thumbnail = data.thumbnailUrl
                    
                    return createFallbackResult(originalUrl, title, artist, thumbnail)
                }
            }
        } catch (e: Exception) {
            // OEmbed failed, continue to next fallback
        }
        
        // 2. Try Direct File Link (MP3, etc.)
        if (isDirectFile(originalUrl)) {
             return tryFileFallback(originalUrl)
        }

        // 3. Generic OpenGraph / Meta Tag Fallback
        return tryGenericFallback(originalUrl)
    }
    
    private suspend fun tryFileFallback(url: String): ResolveResult {
         try {
             // Extract filename from URL
             val fileName = java.net.URLDecoder.decode(url.substringAfterLast("/"), "UTF-8")
             val cleanName = fileName.substringBefore("?")
             
             return createFallbackResult(url, cleanName, "Direct File", null)
         } catch (e: Exception) {
             return ResolveResult.Error("Failed to parse file URL: ${e.message}")
         }
    }

    private suspend fun tryGenericFallback(originalUrl: String): ResolveResult {
        try {
            // Fetch and parse HTML
             val doc = withContext(Dispatchers.IO) {
                 Jsoup.connect(originalUrl)
                     .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                     .timeout(5000)
                     .get()
             }
            
            // Extract metadata from Open Graph tags
            var title = doc.select("meta[property=og:title]").attr("content")
            if (title.isBlank()) title = doc.title()
            
            val image = doc.select("meta[property=og:image]").attr("content")
            
            // Try to find artist/site name
            var description = doc.select("meta[property=og:description]").attr("content")
            val siteName = doc.select("meta[property=og:site_name]").attr("content")
            
            val artistOrSite = if (siteName.isNotBlank()) siteName else description

            if (title.isNotBlank()) {
                 return createFallbackResult(originalUrl, title, artistOrSite, image.ifBlank { null })
            }
            
            return ResolveResult.Error("Could not extract metadata")
            
        } catch (e: Exception) {
            return ResolveResult.Error("All resolutions failed: ${e.message}")
        }
    }

    private fun isDirectFile(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains(".mp3") || 
               lower.contains(".m4a") || 
               lower.contains(".wav") || 
               lower.contains(".aac") ||
               lower.contains(".ogg")
    }

    private suspend fun createFallbackResult(url: String, title: String, artist: String?, thumbnail: String?): ResolveResult {
        // Check for existing item
        val existingItem = historyDao.getHistoryItemByUrl(url)
        val newId = existingItem?.id ?: 0

        val historyItem = HistoryItem(
            id = newId,
            originalUrl = url,
            songTitle = title,
            artistName = artist,
            thumbnailUrl = thumbnail,
            originalImageUrl = thumbnail, // Store original for backup/restore
            linksJson = "{}", // Empty links
            isResolved = true
        )
        historyDao.insert(historyItem)
        
        val searchQuery = if (artist != null) "$title $artist" else title
        return ResolveResult.Fallback(historyItem, searchQuery)
    }

    suspend fun saveUnresolvedLink(url: String) {
        val existingItem = historyDao.getHistoryItemByUrl(url)
        if (existingItem != null && existingItem.isResolved) {
             // If it's already resolved, don't overwrite with unresolved
             return
        }
        
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
    
    private suspend fun tryShazamFallback(originalUrl: String): ResolveResult {
        try {
            // Fetch and parse Shazam page HTML
            val doc = withContext(Dispatchers.IO) {
                Jsoup.connect(originalUrl).get()
            }
            
            // Extract metadata from Open Graph tags
            val ogTitle = doc.select("meta[property=og:title]").attr("content")
            val ogImage = doc.select("meta[property=og:image]").attr("content")
            
            if (ogTitle.isBlank()) {
                return ResolveResult.Error("Could not extract metadata from Shazam page")
            }
            
            // Parse title format: "Song Title - Artist Name: Song Lyrics, Music Videos & Concerts"
            // or "Song Title (feat. Artist) - Main Artist: Song Lyrics..."
            val titleParts = ogTitle.split(" - ", limit = 2)
            val songTitle = titleParts.getOrNull(0)?.trim() ?: "Unknown Title"
            val artistPart = titleParts.getOrNull(1)?.split(":")?.getOrNull(0)?.trim()
            
            val thumbnail = ogImage.ifBlank { null }
            
            return createFallbackResult(originalUrl, songTitle, artistPart, thumbnail)
            
        } catch (e: Exception) {
            return ResolveResult.Error("Failed to parse Shazam page: ${e.message}")
        }
    }

    private fun getOEmbedUrl(url: String): String? {
        return when {
            url.contains("spotify.com") -> "https://open.spotify.com/oembed?url=$url"
            url.contains("music.apple.com") -> "https://music.apple.com/oembed?url=$url" // Note: Apple OEmbed is unofficial/limited, might fail
            else -> null
        }
    }

    fun getTargetUrl(response: OdesliResponse, targetApp: String): String? {
        val links = response.linksByPlatform ?: return null
        
        val platformKey = when (targetApp) {
            SettingsManager.TARGET_SPOTIFY -> "spotify"
            SettingsManager.TARGET_APPLE_MUSIC -> "appleMusic"
            SettingsManager.TARGET_TIDAL -> "tidal"
            SettingsManager.TARGET_AMAZON_MUSIC -> "amazonMusic"
            SettingsManager.TARGET_YOUTUBE_MUSIC -> "youtubeMusic"
            SettingsManager.PLATFORM_DEEZER -> "deezer"
            SettingsManager.PLATFORM_SOUNDCLOUD -> "soundcloud"
            SettingsManager.PLATFORM_NAPSTER -> "napster"
            SettingsManager.PLATFORM_PANDORA -> "pandora"
            SettingsManager.PLATFORM_AUDIOMACK -> "audiomack"
            SettingsManager.PLATFORM_ANGHAMI -> "anghami"
            SettingsManager.PLATFORM_BOOMPLAY -> "boomplay"
            SettingsManager.PLATFORM_YANDEX -> "yandex"
            SettingsManager.PLATFORM_AUDIUS -> "audius"
            SettingsManager.PLATFORM_BANDCAMP -> "bandcamp"
            SettingsManager.PLATFORM_YOUTUBE -> "youtube"
            SettingsManager.TARGET_UNIVERSAL -> null // Will be handled in LinkProcessor
            else -> return response.pageUrl
        } ?: return null

        var link = links[platformKey]
        if (link == null && platformKey == "appleMusic") {
            link = links["itunes"]
        }
        
        // Best Practice: Prefer nativeAppUriMobile for direct app opening, fallback to url
        return link?.nativeAppUriMobile ?: link?.url ?: response.pageUrl
    }
    
    fun getPlatformFromUrl(url: String): String? {
        return when {
            url.contains("spotify.com") -> "spotify"
            url.contains("apple.com") || url.contains("itunes.apple.com") -> "appleMusic"
            url.contains("tidal.com") -> "tidal"
            url.contains("amazon.com") -> "amazonMusic"
            url.contains("music.youtube.com") -> "youtubeMusic"
            url.contains("youtube.com") || url.contains("youtu.be") -> "youtube"
            url.contains("deezer.com") -> "deezer"
            url.contains("soundcloud.com") -> "soundcloud"
            url.contains("napster.com") -> "napster"
            url.contains("pandora.com") -> "pandora"
            url.contains("audiomack.com") -> "audiomack"
            url.contains("yandex.com") || url.contains("yandex.ru") -> "yandex"
            url.contains("anghami.com") -> "anghami"
            url.contains("boomplay.com") -> "boomplay"
            url.contains("audius.co") -> "audius"
            url.contains("bandcamp.com") -> "bandcamp"
            url.contains("shazam.com") -> "shazam"
            else -> null
        }
    }

    fun isPodcastUrl(url: String): Boolean {
        return url.contains("podcasts.apple.com") ||
                url.contains("pca.st") ||
                url.contains("pocketcasts.com") ||
                url.contains("castbox.fm") ||
                url.contains("podcastaddict.com") ||
                url.contains("player.fm") ||
                url.contains("antennapod.org") ||
                url.contains("podbean.com") ||
                url.contains("podcastguru.io") ||
                url.contains("open.spotify.com/episode") ||
                url.contains("open.spotify.com/show")
    }

    fun getPodcastPlatformFromUrl(url: String): String? {
        return when {
            url.contains("podcasts.apple.com") -> SettingsManager.TARGET_PODCAST_APPLE
            url.contains("pca.st") || url.contains("pocketcasts.com") -> SettingsManager.TARGET_PODCAST_POCKET_CASTS
            url.contains("castbox.fm") -> SettingsManager.TARGET_PODCAST_CASTBOX
            url.contains("podcastaddict.com") -> SettingsManager.TARGET_PODCAST_PODCAST_ADDICT
            url.contains("player.fm") -> SettingsManager.TARGET_PODCAST_PLAYER_FM
            url.contains("antennapod.org") -> SettingsManager.TARGET_PODCAST_ANTENNAPOD
            url.contains("podbean.com") -> SettingsManager.TARGET_PODCAST_PODBEAN
            url.contains("podcastguru.io") -> SettingsManager.TARGET_PODCAST_PODCAST_GURU
            url.contains("spotify.com") -> SettingsManager.TARGET_PODCAST_SPOTIFY
            else -> null
        }
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
