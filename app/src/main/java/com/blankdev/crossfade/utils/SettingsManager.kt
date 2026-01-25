package com.blankdev.crossfade.utils

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("crossfade_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TARGET_APP = "key_target_app"
        private const val KEY_HAS_COMPLETED_ONBOARDING = "key_has_completed_onboarding"
        private const val KEY_ADDITIONAL_SERVICES = "key_additional_services"

        
        // Target App Constants
        const val TARGET_SPOTIFY = "spotify"
        const val TARGET_APPLE_MUSIC = "apple_music"
        const val TARGET_TIDAL = "tidal"
        const val TARGET_AMAZON_MUSIC = "amazon_music"
        const val TARGET_YOUTUBE_MUSIC = "youtube_music"
        const val TARGET_UNIVERSAL = "universal"

        // New Platforms supported by Odesli
        const val PLATFORM_DEEZER = "deezer"
        const val PLATFORM_SOUNDCLOUD = "soundcloud"
        const val PLATFORM_NAPSTER = "napster"
        const val PLATFORM_PANDORA = "pandora"
        const val PLATFORM_AUDIOMACK = "audiomack"
        const val PLATFORM_ANGHAMI = "anghami"
        const val PLATFORM_BOOMPLAY = "boomplay"
        const val PLATFORM_YANDEX = "yandex"
        const val PLATFORM_AUDIUS = "audius"
        const val PLATFORM_BANDCAMP = "bandcamp"
        const val PLATFORM_YOUTUBE = "youtube"
        const val PLATFORM_SHAZAM = "shazam"

        // Podcast Target App Constants
        const val TARGET_PODCAST_SPOTIFY = "podcast_spotify"
        const val TARGET_PODCAST_APPLE = "podcast_apple"
        const val TARGET_PODCAST_POCKET_CASTS = "podcast_pocket_casts"
        const val TARGET_PODCAST_CASTBOX = "podcast_castbox"
        const val TARGET_PODCAST_PODCAST_ADDICT = "podcast_addict"
        const val TARGET_PODCAST_PLAYER_FM = "podcast_player_fm"
        const val TARGET_PODCAST_ANTENNAPOD = "podcast_antennapod"
        const val TARGET_PODCAST_PODBEAN = "podcast_podbean"
        const val TARGET_PODCAST_PODCAST_GURU = "podcast_guru"
        const val TARGET_PODCAST_WEB = "podcast_web"

        private const val KEY_TARGET_PODCAST_APP = "key_target_podcast_app"

        // Theme Constants
        const val KEY_THEME_MODE = "key_theme_mode"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        const val THEME_SYSTEM = "system"
        
        // Conflict Resolution
        private const val KEY_USE_ODESLI_FOR_CONFLICTS = "key_use_odesli_for_conflicts"
        
        // Backup Settings
        private const val KEY_AUTO_BACKUP_ENABLED = "key_auto_backup_enabled"
        private const val KEY_AUTO_BACKUP_FREQUENCY = "key_auto_backup_frequency"
        private const val KEY_AUTO_BACKUP_FOLDER_URI = "key_auto_backup_folder_uri"
        private const val KEY_LAST_BACKUP_TIMESTAMP = "key_last_backup_timestamp"
        
        const val BACKUP_FREQUENCY_DAILY = "daily"
        const val BACKUP_FREQUENCY_WEEKLY = "weekly"
        const val BACKUP_FREQUENCY_MONTHLY = "monthly"
    }

    var targetApp: String
        get() = prefs.getString(KEY_TARGET_APP, TARGET_UNIVERSAL) ?: TARGET_UNIVERSAL
        set(value) = prefs.edit().putString(KEY_TARGET_APP, value).apply()

    var targetPodcastApp: String
        get() = prefs.getString(KEY_TARGET_PODCAST_APP, TARGET_PODCAST_SPOTIFY) ?: TARGET_PODCAST_SPOTIFY
        set(value) = prefs.edit().putString(KEY_TARGET_PODCAST_APP, value).apply()

    var themeMode: String
        get() = prefs.getString(KEY_THEME_MODE, THEME_SYSTEM) ?: THEME_SYSTEM
        set(value) = prefs.edit().putString(KEY_THEME_MODE, value).apply()


    var additionalServices: Set<String>
        get() = prefs.getStringSet(KEY_ADDITIONAL_SERVICES, setOf("youtubeMusic")) ?: setOf("youtubeMusic")
        set(value) = prefs.edit().putStringSet(KEY_ADDITIONAL_SERVICES, value).apply()

    var hasCompletedOnboarding: Boolean
        get() = prefs.getBoolean(KEY_HAS_COMPLETED_ONBOARDING, false)
        set(value) = prefs.edit().putBoolean(KEY_HAS_COMPLETED_ONBOARDING, value).apply()
    
    var useOdesliForConflicts: Boolean
        get() = prefs.getBoolean(KEY_USE_ODESLI_FOR_CONFLICTS, false)
        set(value) = prefs.edit().putBoolean(KEY_USE_ODESLI_FOR_CONFLICTS, value).apply()
    
    var autoBackupEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_BACKUP_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_BACKUP_ENABLED, value).apply()
    
    var autoBackupFrequency: String
        get() = prefs.getString(KEY_AUTO_BACKUP_FREQUENCY, BACKUP_FREQUENCY_WEEKLY) ?: BACKUP_FREQUENCY_WEEKLY
        set(value) = prefs.edit().putString(KEY_AUTO_BACKUP_FREQUENCY, value).apply()
    
    var autoBackupFolderUri: String?
        get() = prefs.getString(KEY_AUTO_BACKUP_FOLDER_URI, null)
        set(value) = prefs.edit().putString(KEY_AUTO_BACKUP_FOLDER_URI, value).apply()
    
    var lastBackupTimestamp: Long
        get() = prefs.getLong(KEY_LAST_BACKUP_TIMESTAMP, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_BACKUP_TIMESTAMP, value).apply()
    
    /**
     * Get all settings as a map for backup export
     */
    fun getAllSettings(): Map<String, String> {
        return mapOf(
            "targetApp" to targetApp,
            "targetPodcastApp" to targetPodcastApp,
            "themeMode" to themeMode,
            "additionalServices" to additionalServices.joinToString(","),
            "hasCompletedOnboarding" to hasCompletedOnboarding.toString(),
            "useOdesliForConflicts" to useOdesliForConflicts.toString(),
            "autoBackupEnabled" to autoBackupEnabled.toString(),
            "autoBackupFrequency" to autoBackupFrequency,
            "autoBackupFolderUri" to (autoBackupFolderUri ?: ""),
            "lastBackupTimestamp" to lastBackupTimestamp.toString()
        )
    }
}
