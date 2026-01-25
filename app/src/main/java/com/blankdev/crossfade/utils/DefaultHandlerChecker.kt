package com.blankdev.crossfade.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

object DefaultHandlerChecker {

    enum class ServiceStatus {
        ACTIVE,
        PARTIAL,
        INACTIVE
    }
    
    // Map platform IDs to a list of representative URLs for checking default handlers
    // These should match the intent filters in AndroidManifest.xml
    private val platformTestUrls = mapOf(
        SettingsManager.TARGET_SPOTIFY to listOf(
            "https://open.spotify.com/track/test",
            "https://spotify.com/track/test",
            "http://open.spotify.com/track/test"
        ),
        SettingsManager.TARGET_APPLE_MUSIC to listOf(
            "https://music.apple.com/us/album/test",
            "https://itunes.apple.com/us/album/test",
            "http://music.apple.com/us/album/test"
        ),
        SettingsManager.TARGET_TIDAL to listOf(
            "https://tidal.com/browse/track/test",
            "https://www.tidal.com/browse/track/test",
            "http://tidal.com/browse/track/test",
            "https://listen.tidal.com/track/test"
        ),
        SettingsManager.TARGET_AMAZON_MUSIC to listOf(
            "https://music.amazon.com/albums/test",
            "http://music.amazon.com/albums/test"
        ),
        SettingsManager.TARGET_YOUTUBE_MUSIC to listOf(
            "https://music.youtube.com/watch?v=test",
            "http://music.youtube.com/watch?v=test"
        ),
        SettingsManager.PLATFORM_DEEZER to listOf(
            "https://www.deezer.com/track/test",
            "https://deezer.com/track/test",
            "http://www.deezer.com/track/test"
        ),
        SettingsManager.PLATFORM_SOUNDCLOUD to listOf(
            "https://soundcloud.com/artist/track",
            "https://www.soundcloud.com/artist/track",
            "https://on.soundcloud.com/track",
            "http://soundcloud.com/artist/track"
        ),
        SettingsManager.PLATFORM_NAPSTER to listOf(
            "https://napster.com/artist/track",
            "https://www.napster.com/artist/track",
            "https://us.napster.com/artist/track",
            "https://web.napster.com/artist/track",
            "http://napster.com/artist/track"
        ),
        SettingsManager.PLATFORM_PANDORA to listOf(
            "https://www.pandora.com/artist/track",
            "https://pandora.com/artist/track",
            "http://www.pandora.com/artist/track"
        ),
        SettingsManager.PLATFORM_AUDIOMACK to listOf(
            "https://audiomack.com/song/test",
            "https://www.audiomack.com/song/test",
            "http://audiomack.com/song/test"
        ),
        SettingsManager.PLATFORM_ANGHAMI to listOf(
            "https://play.anghami.com/song/test",
            "https://anghami.com/song/test",
            "https://www.anghami.com/song/test",
            "http://play.anghami.com/song/test"
        ),
        SettingsManager.PLATFORM_BOOMPLAY to listOf(
            "https://www.boomplay.com/songs/test",
            "https://boomplay.com/songs/test",
            "http://www.boomplay.com/songs/test"
        ),
        SettingsManager.PLATFORM_YANDEX to listOf(
            "https://music.yandex.com/album/test/track/test",
            "https://music.yandex.ru/album/test/track/test",
            "http://music.yandex.com/album/test/track/test"
        ),
        SettingsManager.PLATFORM_AUDIUS to listOf(
            "https://audius.co/artist/track",
            "https://www.audius.co/artist/track",
            "http://audius.co/artist/track"
        ),
        SettingsManager.PLATFORM_BANDCAMP to listOf(
            "https://bandcamp.com/track/test",
            "https://www.bandcamp.com/track/test",
            "http://bandcamp.com/track/test"
        ),
         SettingsManager.PLATFORM_SHAZAM to listOf(
            "https://shazam.com/track/test",
            "https://www.shazam.com/track/test",
            "http://shazam.com/track/test"
        )
    )
    
    /**
     * Check if Crossfade is the default handler for a specific platform
     * This is a simple check that returns true if ANY of the representative URLs are handled.
     * Use checkServiceStatus for more granular detail.
     * @param context Application context
     * @param platformId Platform ID from SettingsManager (e.g., TARGET_SPOTIFY)
     * @return true if Crossfade is the default handler for this platform
     */
    fun isCrossfadeDefaultHandler(context: Context, platformId: String): Boolean {
        val urls = platformTestUrls[platformId] ?: return false
        
        // Return true if at least one URL is handled
        return urls.any { url ->
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            val resolveInfo = context.packageManager.resolveActivity(
                intent, 
                PackageManager.MATCH_DEFAULT_ONLY
            )
            resolveInfo?.activityInfo?.packageName == context.packageName
        }
    }

    /**
     * Check the status of default handling for a service
     * @param context Application context
     * @param platformId Platform ID
     * @return ServiceStatus: ACTIVE (all links), PARTIAL (some links), INACTIVE (no links)
     */
    fun checkServiceStatus(context: Context, platformId: String): ServiceStatus {
        val urls = platformTestUrls[platformId] ?: return ServiceStatus.INACTIVE
        if (urls.isEmpty()) return ServiceStatus.INACTIVE

        var handledCount = 0
        
        for (url in urls) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            val resolveInfo = context.packageManager.resolveActivity(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
            if (resolveInfo?.activityInfo?.packageName == context.packageName) {
                handledCount++
            }
        }

        return when {
            handledCount == urls.size -> ServiceStatus.ACTIVE
            handledCount > 0 -> ServiceStatus.PARTIAL
            else -> ServiceStatus.INACTIVE
        }
    }

    /**
     * Get the number of services (platforms) handled by Crossfade, optionally excluding a specific platform.
     */
    fun getHandledLinksCount(context: Context, excludePlatformId: String? = null): Int {
        var count = 0
        for (platformId in platformTestUrls.keys) {
            if (platformId == excludePlatformId) continue
            
            if (isCrossfadeDefaultHandler(context, platformId)) {
                count++
            }
        }
        return count
    }

    /**
     * Get the total number of supported services, excluding a specific platform.
     */
    fun getTotalRelevantLinksCount(excludePlatformId: String? = null): Int {
        var count = 0
        for (platformId in platformTestUrls.keys) {
            if (platformId == excludePlatformId) continue
            count++
        }
        return count
    }
    
    /**
     * Check if there's a conflict between the preferred app and Crossfade's default handlers
     * @param context Application context
     * @param preferredApp The preferred app platform ID
     * @return true if Crossfade handles the preferred app's platform, creating a potential infinite loop
     */
    fun hasConflict(context: Context, preferredApp: String): Boolean {
        // "Ask everytime" (TARGET_UNIVERSAL) never conflicts
        if (preferredApp == SettingsManager.TARGET_UNIVERSAL) {
            return false
        }
        
        return isCrossfadeDefaultHandler(context, preferredApp)
    }
    
    /**
     * Get a user-friendly platform name for display in warnings
     */
    fun getPlatformDisplayName(platformId: String): String {
        return when (platformId) {
            SettingsManager.TARGET_SPOTIFY -> "Spotify"
            SettingsManager.TARGET_APPLE_MUSIC -> "Apple Music"
            SettingsManager.TARGET_TIDAL -> "Tidal"
            SettingsManager.TARGET_AMAZON_MUSIC -> "Amazon Music"
            SettingsManager.TARGET_YOUTUBE_MUSIC -> "YouTube Music"
            SettingsManager.PLATFORM_DEEZER -> "Deezer"
            SettingsManager.PLATFORM_SOUNDCLOUD -> "SoundCloud"
            SettingsManager.PLATFORM_NAPSTER -> "Napster"
            SettingsManager.PLATFORM_PANDORA -> "Pandora"
            SettingsManager.PLATFORM_AUDIOMACK -> "Audiomack"
            SettingsManager.PLATFORM_ANGHAMI -> "Anghami"
            SettingsManager.PLATFORM_BOOMPLAY -> "Boomplay"
            SettingsManager.PLATFORM_YANDEX -> "Yandex Music"
            SettingsManager.PLATFORM_AUDIUS -> "Audius"
            SettingsManager.PLATFORM_BANDCAMP -> "Bandcamp"
            SettingsManager.PLATFORM_SHAZAM, "shazam" -> "Shazam"
            else -> "Unknown Platform"
        }
    }
}
