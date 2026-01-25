package com.blankdev.crossfade.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.blankdev.crossfade.CrossfadeApp
import com.blankdev.crossfade.data.ResolveResult
import com.blankdev.crossfade.utils.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object LinkProcessor {

    fun processUrl(
        context: Context,
        scope: CoroutineScope,
        url: String,
        forceNavigate: Boolean = false,
        historyItem: com.blankdev.crossfade.data.HistoryItem? = null,
        onComplete: (Boolean) -> Unit = {}
    ) {
        val app = CrossfadeApp.instance
        val settings = app.settingsManager
        val resolver = app.linkResolver

        val isPodcast = resolver.isPodcastUrl(url)
        val sourcePlatform = if (isPodcast) resolver.getPodcastPlatformFromUrl(url) else resolver.getPlatformFromUrl(url)
        val targetApp = if (isPodcast) SettingsManager.TARGET_PODCAST_WEB else settings.targetApp

        val shouldNavigate = if (forceNavigate) true else (sourcePlatform != targetApp)

        scope.launch {
            // Optimization: If we already failed to resolve this, don't try again unless forced
            // OR if it's resolved but has no links (like a Shazam fallback)
            val hasNoLinks = historyItem?.isResolved == true && (historyItem.linksJson == null || historyItem.linksJson == "{}")
            val isUnresolved = historyItem?.isResolved == false

            if (historyItem != null && (isUnresolved || hasNoLinks)) {
                withContext(Dispatchers.Main) {
                    if (shouldNavigate) {
                        val searchQuery = if (!historyItem.songTitle.isNullOrBlank()) {
                            if (!historyItem.artistName.isNullOrBlank()) "${historyItem.songTitle} ${historyItem.artistName}" else historyItem.songTitle
                        } else {
                            url
                        }
                        
                        Toast.makeText(context, "Search instead...", Toast.LENGTH_SHORT).show()
                        if (isPodcast) {
                            performPodcastSearch(context, searchQuery, targetApp)
                        } else {
                            performSearch(context, searchQuery, targetApp)
                        }
                        onComplete(true)
                    } else {
                        onComplete(false)
                    }
                }
                return@launch
            }

            // Moved toast inside resolver check to only show it if it's NOT in cache
            // But wait, resolveLink is a suspend fun that does the check.
            // We can't know if it's in cache without calling it.
            // However, we can check the DB here too for UI purposes if we want to be instant.
            
            val cachedItem = withContext(Dispatchers.IO) { app.database.historyDao().getHistoryItemByUrl(url) }
            if (cachedItem == null || !cachedItem.isResolved) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Crossfading...", Toast.LENGTH_SHORT).show()
                }
            }

            val result = resolver.resolveLink(url)
            
            withContext(Dispatchers.Main) {
                when (result) {
                    is ResolveResult.Success -> {
                        if (targetApp == SettingsManager.TARGET_UNIVERSAL) {
                             showMenu(context, result.historyItem)
                             onComplete(true)
                             return@withContext
                        }

                        if (shouldNavigate) {
                            // Check for infinite loop: if Crossfade is the default handler for the preferred app's platform
                            val hasConflict = DefaultHandlerChecker.hasConflict(context, targetApp)
                            
                            if (hasConflict) {
                                // Fallback to Odesli to prevent infinite loop
                                Toast.makeText(context, "Conflict detected, opening Odesli instead", Toast.LENGTH_LONG).show()
                                openUrl(context, result.data.pageUrl ?: "https://odesli.co/")
                                onComplete(true)
                                return@withContext
                            }
                            
                            val targetUrl = if (isPodcast) {
                                resolver.getPodcastTargetUrl(result.data, targetApp)
                            } else {
                                resolver.getTargetUrl(result.data, targetApp)
                            }

                            if (targetUrl != null) {
                                openUrl(context, targetUrl)
                                onComplete(true)
                            } else {
                                Toast.makeText(context, "Link not found for target, opening web...", Toast.LENGTH_SHORT).show()
                                openUrl(context, result.data.pageUrl ?: url)
                                onComplete(true)
                            }
                        } else {
                            Toast.makeText(context, "Link saved! (Source matches Target)", Toast.LENGTH_SHORT).show()
                            onComplete(false)
                        }
                    }
                    is ResolveResult.Fallback -> {
                        if (targetApp == SettingsManager.TARGET_UNIVERSAL) {
                             showMenu(context, result.historyItem)
                             onComplete(true)
                             return@withContext
                        }

                        Toast.makeText(context, "Exact match failed. Searching...", Toast.LENGTH_SHORT).show()
                        if (shouldNavigate) {
                            if (isPodcast) {
                                performPodcastSearch(context, result.searchQuery, targetApp)
                            } else {
                                performSearch(context, result.searchQuery, targetApp)
                            }
                            onComplete(true)
                        } else {
                            onComplete(false)
                        }
                    }
                    is ResolveResult.Error -> {
                        resolver.saveUnresolvedLink(url)
                        // For truly unresolved links, we also show the menu if "Ask Everytime" is on
                        // Wait, resolver.saveUnresolvedLink doesn't return the item. 
                        // But we can fetch it.
                        Toast.makeText(context, "Link unresolved, saved to list", Toast.LENGTH_SHORT).show()
                        
                        if (targetApp == SettingsManager.TARGET_UNIVERSAL) {
                             // Fetch newly saved item to show menu
                             scope.launch {
                                 val item = app.database.historyDao().getHistoryItemByUrl(url)
                                 withContext(Dispatchers.Main) {
                                     if (item != null) showMenu(context, item)
                                 }
                             }
                        }
                        
                        onComplete(false)
                    }
                }
            }
        }
    }

    private fun showMenu(context: Context, item: com.blankdev.crossfade.data.HistoryItem) {
        if (context is androidx.fragment.app.FragmentActivity) {
            val sheet = com.blankdev.crossfade.ui.ShareBottomSheet.newInstance(item)
            sheet.show(context.supportFragmentManager, "ShareSheet")
        }
    }

    fun openUrl(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
        }
    }

    private fun performSearch(context: Context, query: String, targetApp: String) {
        val uriStr = when (targetApp) {
            SettingsManager.TARGET_SPOTIFY -> "spotify:search:${Uri.encode(query)}"
            SettingsManager.TARGET_APPLE_MUSIC -> "https://music.apple.com/us/search?term=${Uri.encode(query)}"
            SettingsManager.TARGET_YOUTUBE_MUSIC -> "https://music.youtube.com/search?q=${Uri.encode(query)}"
            SettingsManager.PLATFORM_DEEZER -> "deezer://www.deezer.com/search/${Uri.encode(query)}"
            SettingsManager.PLATFORM_SOUNDCLOUD -> "https://soundcloud.com/search?q=${Uri.encode(query)}"
            SettingsManager.PLATFORM_NAPSTER -> "https://web.napster.com/search?query=${Uri.encode(query)}"
            SettingsManager.PLATFORM_PANDORA -> "https://www.pandora.com/search/${Uri.encode(query)}"
            SettingsManager.PLATFORM_AUDIOMACK -> "https://audiomack.com/search?q=${Uri.encode(query)}"
            SettingsManager.PLATFORM_YANDEX -> "https://music.yandex.ru/search?text=${Uri.encode(query)}"
            SettingsManager.PLATFORM_BANDCAMP -> "https://bandcamp.com/search?q=${Uri.encode(query)}"
            SettingsManager.PLATFORM_YOUTUBE -> "https://www.youtube.com/results?search_query=${Uri.encode(query)}"
            else -> "https://odesli.co/?q=${Uri.encode(query)}"
        }
        
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriStr)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://odesli.co/?q=${Uri.encode(query)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        }
    }

    private fun performPodcastSearch(context: Context, query: String, targetApp: String) {
        // "Secret" feature: Always open Odesli for podcasts
        val uriStr = "https://odesli.co/?q=${Uri.encode("$query podcast")}"

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriStr)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
             Toast.makeText(context, "Could not open browser", Toast.LENGTH_SHORT).show()
        }
    }
}
