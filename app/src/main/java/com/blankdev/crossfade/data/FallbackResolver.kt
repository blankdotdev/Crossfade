package com.blankdev.crossfade.data

import com.blankdev.crossfade.api.OEmbedApi
import com.blankdev.crossfade.api.PlatformLink
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.URLDecoder

class FallbackResolver(
    private val historyDao: HistoryDao,
    private val oEmbedApi: OEmbedApi
) {

    suspend fun tryFallback(originalUrl: String): ResolveResult {
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
        return try {
            // Extract filename from URL
            val fileName = URLDecoder.decode(url.substringAfterLast("/"), "UTF-8")
            val cleanName = fileName.substringBefore("?")

            createFallbackResult(url, cleanName, "Direct File", null)
        } catch (e: Exception) {
            ResolveResult.Error("Failed to parse file URL: ${e.message}")
        }
    }

    private suspend fun tryGenericFallback(originalUrl: String): ResolveResult {
        return try {
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
            val description = doc.select("meta[property=og:description]").attr("content")
            val siteName = doc.select("meta[property=og:site_name]").attr("content")

            // Clean metadata: exclude known platform names from being used as the artist name
            val ignoredSiteNames = listOf(
                "Spotify", "Apple Music", "Tidal", "Amazon Music", "YouTube Music",
                "Deezer", "SoundCloud", "Napster", "Pandora", "Audiomack",
                "Anghami", "Boomplay", "Yandex Music", "Audius", "Bandcamp", "Shazam"
            )

            val artistOrSite = when {
                siteName.isNotBlank() && !ignoredSiteNames.any { siteName.equals(it, ignoreCase = true) } -> siteName
                description.isNotBlank() && description.length < 100 -> description // Use short descriptions as potential artist info
                else -> null
            }

            if (title.isNotBlank()) {
                createFallbackResult(originalUrl, title, artistOrSite, image.ifBlank { null })
            } else {
                ResolveResult.Error("Could not extract metadata")
            }
        } catch (e: Exception) {
            ResolveResult.Error("All resolutions failed: ${e.message}")
        }
    }

    private suspend fun tryShazamFallback(originalUrl: String): ResolveResult {
        return try {
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
            val titleParts = ogTitle.split(" - ", limit = 2)
            val songTitle = titleParts.getOrNull(0)?.trim() ?: "Unknown Title"
            val artistPart = titleParts.getOrNull(1)?.split(":")?.getOrNull(0)?.trim()

            val thumbnail = ogImage.ifBlank { null }

            createFallbackResult(originalUrl, songTitle, artistPart, thumbnail)
        } catch (e: Exception) {
            ResolveResult.Error("Failed to parse Shazam page: ${e.message}")
        }
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
            originalImageUrl = thumbnail,
            linksJson = "{}", // Empty links
            isResolved = true
        )
        val savedId = historyDao.insert(historyItem)
        val historyItemWithId = historyItem.copy(id = savedId)

        // Aggressive cleanup for search query
        val searchQuery = cleanSearchQuery(title, artist)

        return ResolveResult.Fallback(historyItemWithId, searchQuery)
    }

    private fun cleanSearchQuery(title: String, artist: String?): String {
        val platforms = listOf(
            "Spotify", "Apple Music", "Tidal", "Amazon Music", "YouTube Music", "YouTube",
            "Deezer", "SoundCloud", "Napster", "Pandora", "Audiomack",
            "Anghami", "Boomplay", "Yandex Music", "Audius", "Bandcamp", "Shazam"
        )

        var cleanTitle = title
        var cleanArtist = artist ?: ""

        platforms.forEach { platform ->
            val regex = "(?i)[\\s\\-\\|]*\\b$platform\\b[\\s\\-\\|]*".toRegex()
            cleanTitle = cleanTitle.replace(regex, " ").trim()
            cleanArtist = cleanArtist.replace(regex, " ").trim()
        }

        return if (cleanArtist.isNotBlank()) "$cleanTitle $cleanArtist" else cleanTitle
    }

    private fun isDirectFile(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains(".mp3") ||
                lower.contains(".m4a") ||
                lower.contains(".wav") ||
                lower.contains(".aac") ||
                lower.contains(".ogg")
    }

    private fun getOEmbedUrl(url: String): String? {
        return when {
            url.contains("spotify.com") -> "https://open.spotify.com/oembed?url=$url"
            url.contains("music.apple.com") -> "https://music.apple.com/oembed?url=$url"
            else -> null
        }
    }
}
