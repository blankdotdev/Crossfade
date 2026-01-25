package com.blankdev.crossfade

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.util.DebugLogger
import com.blankdev.crossfade.data.AppDatabase
import com.blankdev.crossfade.data.LinkResolver
import com.blankdev.crossfade.utils.SettingsManager

class CrossfadeApp : Application(), ImageLoaderFactory {
    
    lateinit var settingsManager: SettingsManager
    lateinit var linkResolver: LinkResolver
    lateinit var database: AppDatabase

    override fun onCreate() {
        super.onCreate()
        settingsManager = SettingsManager(this)
        database = AppDatabase.getDatabase(this)
        linkResolver = LinkResolver(database.historyDao())
        
        instance = this
        
        // Apply Theme
        val mode = when (settingsManager.themeMode) {
            SettingsManager.THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            SettingsManager.THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)

        // Initialize things if needed
        com.google.android.material.color.DynamicColors.applyToActivitiesIfAvailable(this)
    }
    
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25) // Use 25% of available memory
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.10) // Use 10% of disk space
                    .build()
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .crossfade(150) // Smooth crossfade animation
            .respectCacheHeaders(false)
            .build()
    }
    
    companion object {
        lateinit var instance: CrossfadeApp
            private set
    }
}
