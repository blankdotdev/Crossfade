package com.blankdev.crossfade.utils

import com.blankdev.crossfade.R

data class PlatformInfo(
    val internalId: String,
    val displayName: String,
    val iconResId: Int,
    val odesliKey: String?,
    val searchUrlTemplate: String? = null,
    val podcastUrlTemplate: String? = null
)

object PlatformRegistry {

    val PLATFORMS = listOf(
        PlatformInfo(
            SettingsManager.TARGET_SPOTIFY,
            "Spotify",
            R.drawable.ic_spotify,
            "spotify",
            "spotify:search:%s"
        ),
        PlatformInfo(
            SettingsManager.TARGET_APPLE_MUSIC,
            "Apple Music",
            R.drawable.ic_apple_music,
            "appleMusic",
            "https://music.apple.com/us/search?term=%s"
        ),
        PlatformInfo(
            SettingsManager.TARGET_TIDAL,
            "Tidal",
            R.drawable.ic_tidal,
            "tidal",
            "https://listen.tidal.com/search/%s"
        ),
        PlatformInfo(
            SettingsManager.TARGET_AMAZON_MUSIC,
            "Amazon Music",
            R.drawable.ic_amazon_music,
            "amazonMusic",
            "https://music.amazon.com/search/%s"
        ),
        PlatformInfo(
            SettingsManager.TARGET_YOUTUBE_MUSIC,
            "YouTube Music",
            R.drawable.ic_youtube_music,
            "youtubeMusic",
            "https://music.youtube.com/search?q=%s"
        ),
        PlatformInfo(
            SettingsManager.PLATFORM_DEEZER,
            "Deezer",
            R.drawable.ic_deezer,
            "deezer",
            "deezer://www.deezer.com/search/%s"
        ),
        PlatformInfo(
            SettingsManager.PLATFORM_SOUNDCLOUD,
            "SoundCloud",
            R.drawable.ic_soundcloud,
            "soundcloud",
            "https://soundcloud.com/search?q=%s"
        ),
        PlatformInfo(
            SettingsManager.PLATFORM_NAPSTER,
            "Napster",
            R.drawable.ic_napster,
            "napster",
            "https://web.napster.com/search?query=%s"
        ),
        PlatformInfo(
            SettingsManager.PLATFORM_PANDORA,
            "Pandora",
            R.drawable.ic_pandora,
            "pandora",
            "https://www.pandora.com/search/%s"
        ),
        PlatformInfo(
            SettingsManager.PLATFORM_AUDIOMACK,
            "Audiomack",
            R.drawable.ic_audiomack,
            "audiomack",
            "https://audiomack.com/search?q=%s"
        ),
        PlatformInfo(
            SettingsManager.PLATFORM_ANGHAMI,
            "Anghami",
            R.drawable.ic_anghami,
            "anghami",
            "https://play.anghami.com/search/%s"
        ),
        PlatformInfo(
            SettingsManager.PLATFORM_BOOMPLAY,
            "Boomplay",
            R.drawable.ic_boomplay,
            "boomplay",
            "https://www.boomplay.com/search/%s"
        ),
        PlatformInfo(
            SettingsManager.PLATFORM_YANDEX,
            "Yandex Music",
            R.drawable.ic_yandex_music,
            "yandex",
            "https://music.yandex.ru/search?text=%s"
        ),
        PlatformInfo(
            SettingsManager.PLATFORM_AUDIUS,
            "Audius",
            R.drawable.ic_audius,
            "audius",
            "https://audius.co/search/%s"
        ),
        PlatformInfo(
            SettingsManager.PLATFORM_BANDCAMP,
            "Bandcamp",
            R.drawable.ic_bandcamp,
            "bandcamp",
            "https://bandcamp.com/search?q=%s"
        ),
        PlatformInfo(
            SettingsManager.PLATFORM_YOUTUBE,
            "YouTube",
            R.drawable.ic_youtube,
            "youtube",
            "https://www.youtube.com/results?search_query=%s"
        ),
        PlatformInfo(
            SettingsManager.PLATFORM_SHAZAM,
            "Shazam",
            R.drawable.ic_shazam,
            "shazam"
        )
    )

    private val podcastPlatforms = listOf(
        PlatformInfo(SettingsManager.TARGET_PODCAST_SPOTIFY, "Spotify", R.drawable.ic_spotify, "spotify"),
        PlatformInfo(SettingsManager.TARGET_PODCAST_APPLE, "Apple Podcasts", R.drawable.ic_apple_music, "appleMusic"),
        PlatformInfo(SettingsManager.TARGET_PODCAST_POCKET_CASTS, "Pocket Casts", R.drawable.ic_placeholder_service, null),
        PlatformInfo(SettingsManager.TARGET_PODCAST_CASTBOX, "Castbox", R.drawable.ic_placeholder_service, null),
        PlatformInfo(SettingsManager.TARGET_PODCAST_PODCAST_ADDICT, "Podcast Addict", R.drawable.ic_placeholder_service, null),
        PlatformInfo(SettingsManager.TARGET_PODCAST_PLAYER_FM, "Player FM", R.drawable.ic_placeholder_service, null),
        PlatformInfo(SettingsManager.TARGET_PODCAST_ANTENNAPOD, "AntennaPod", R.drawable.ic_placeholder_service, null),
        PlatformInfo(SettingsManager.TARGET_PODCAST_PODBEAN, "Podbean", R.drawable.ic_placeholder_service, null),
        PlatformInfo(SettingsManager.TARGET_PODCAST_PODCAST_GURU, "Podcast Guru", R.drawable.ic_placeholder_service, null)
    )

    fun getPlatformByInternalId(id: String): PlatformInfo? {
        return PLATFORMS.find { it.internalId == id } ?: podcastPlatforms.find { it.internalId == id }
    }

    fun getPlatformByOdesliKey(key: String): PlatformInfo? {
        return PLATFORMS.find { it.odesliKey == key } ?: podcastPlatforms.find { it.odesliKey == key }
    }

    fun getPlatformFromUrl(url: String): String? {
        return when {
            url.contains("spotify.com") -> SettingsManager.TARGET_SPOTIFY
            url.contains("apple.com") || url.contains("itunes.apple.com") -> SettingsManager.TARGET_APPLE_MUSIC
            url.contains("tidal.com") -> SettingsManager.TARGET_TIDAL
            url.contains("amazon.com") -> SettingsManager.TARGET_AMAZON_MUSIC
            url.contains("music.youtube.com") -> SettingsManager.TARGET_YOUTUBE_MUSIC
            url.contains("youtube.com") || url.contains("youtu.be") -> SettingsManager.PLATFORM_YOUTUBE
            url.contains("deezer.com") -> SettingsManager.PLATFORM_DEEZER
            url.contains("soundcloud.com") -> SettingsManager.PLATFORM_SOUNDCLOUD
            url.contains("napster.com") -> SettingsManager.PLATFORM_NAPSTER
            url.contains("pandora.com") -> SettingsManager.PLATFORM_PANDORA
            url.contains("audiomack.com") -> SettingsManager.PLATFORM_AUDIOMACK
            url.contains("yandex.com") || url.contains("yandex.ru") -> SettingsManager.PLATFORM_YANDEX
            url.contains("anghami.com") -> SettingsManager.PLATFORM_ANGHAMI
            url.contains("boomplay.com") -> SettingsManager.PLATFORM_BOOMPLAY
            url.contains("audius.co") -> SettingsManager.PLATFORM_AUDIUS
            url.contains("bandcamp.com") -> SettingsManager.PLATFORM_BANDCAMP
            url.contains("shazam.com") -> SettingsManager.PLATFORM_SHAZAM
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
}
