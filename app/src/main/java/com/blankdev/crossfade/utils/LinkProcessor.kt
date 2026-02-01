package com.blankdev.crossfade.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.blankdev.crossfade.CrossfadeApp
import com.blankdev.crossfade.data.ResolveResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.blankdev.crossfade.ui.MainActivity
import com.blankdev.crossfade.ui.RedirectActivity
import com.blankdev.crossfade.ui.ShareBottomSheet

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

        val isPodcast = PlatformRegistry.isPodcastUrl(url)
        val sourcePlatform = if (isPodcast) PlatformRegistry.getPodcastPlatformFromUrl(url) else PlatformRegistry.getPlatformFromUrl(url)
        val targetApp = if (isPodcast) SettingsManager.TARGET_PODCAST_WEB else settings.targetApp

        val shouldNavigate = if (forceNavigate) true else (sourcePlatform != targetApp)

        scope.launch {
            val hasNoLinks = historyItem?.isResolved == true && (historyItem.linksJson == null || historyItem.linksJson == "{}")
            val isUnresolved = historyItem?.isResolved == false

            if (historyItem != null && (isUnresolved || hasNoLinks)) {
                withContext(Dispatchers.Main) {
                    if (shouldNavigate) {
                        if (context is androidx.fragment.app.FragmentActivity) {
                            com.blankdev.crossfade.ui.ShareBottomSheet.showResolveFlow(
                                context.supportFragmentManager,
                                historyItem
                            ) { updatedItem ->
                                handleResolutionSuccess(context, updatedItem)
                            }
                        } else {
                            val searchQuery = if (!historyItem.songTitle.isNullOrBlank()) {
                                if (!historyItem.artistName.isNullOrBlank()) "${historyItem.songTitle} ${historyItem.artistName}" else historyItem.songTitle
                            } else {
                                url
                            }
                            Toast.makeText(context, "Search instead...", Toast.LENGTH_SHORT).show()
                            performSearch(context, searchQuery, targetApp)
                        }
                        onComplete(true)
                    } else {
                        onComplete(false)
                    }
                }
                return@launch
            }

            val cachedItem = withContext(Dispatchers.IO) { app.database.historyDao().getHistoryItemByUrl(url) }
            val isActuallyResolved = cachedItem != null && cachedItem.isResolved && !cachedItem.linksJson.isNullOrBlank() && cachedItem.linksJson != "{}"
            
            if (!isActuallyResolved) {
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
                            val hasConflict = DefaultHandlerChecker.hasConflict(context, targetApp)
                            
                            if (hasConflict) {
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

                        if (shouldNavigate) {
                            if (context is androidx.fragment.app.FragmentActivity) {
                                if (context is RedirectActivity) {
                                    redirectToFixMatch(context, url, result.historyItem)
                                    onComplete(false)
                                } else {
                                    ShareBottomSheet.showResolveFlow(
                                        context.supportFragmentManager,
                                        result.historyItem
                                    ) { updatedItem ->
                                        handleResolutionSuccess(context, updatedItem)
                                    }
                                    onComplete(true)
                                }
                            } else {
                                Toast.makeText(context, "Exact match failed. Searching...", Toast.LENGTH_SHORT).show()
                                performSearch(context, result.searchQuery, targetApp)
                                onComplete(true)
                            }
                        } else {
                            onComplete(false)
                        }
                    }
                    is ResolveResult.Error -> {
                        resolver.saveUnresolvedLink(url)
                        
                        if (context is androidx.fragment.app.FragmentActivity) {
                            if (context is RedirectActivity) {
                                val item = app.database.historyDao().getHistoryItemByUrl(url)
                                redirectToFixMatch(context, url, item)
                                onComplete(false)
                            } else {
                                scope.launch {
                                    val item = app.database.historyDao().getHistoryItemByUrl(url)
                                    withContext(Dispatchers.Main) {
                                        if (item != null) {
                                            ShareBottomSheet.showResolveFlow(
                                                context.supportFragmentManager,
                                                item
                                            ) { updatedItem ->
                                                handleResolutionSuccess(context, updatedItem)
                                            }
                                        } else {
                                            Toast.makeText(context, "Link unresolved, saved to list", Toast.LENGTH_SHORT).show()
                                        }
                                        onComplete(false)
                                    }
                                }
                            }
                        } else {
                            Toast.makeText(context, "Link unresolved, saved to list", Toast.LENGTH_SHORT).show()
                            onComplete(false)
                        }
                    }
                }
            }
        }
    }

    private fun redirectToFixMatch(context: Context, url: String, item: com.blankdev.crossfade.data.HistoryItem? = null) {
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            action = MainActivity.ACTION_FIX_MATCH
            putExtra(MainActivity.EXTRA_URL, url)
            if (item != null) {
                putExtra(MainActivity.EXTRA_ITEM_JSON, Gson().toJson(item))
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        context.startActivity(mainIntent)
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
        val platform = PlatformRegistry.getPlatformByInternalId(targetApp)
        val uriStr = if (platform?.searchUrlTemplate != null) {
            platform.searchUrlTemplate.format(Uri.encode(query))
        } else {
            "https://odesli.co/?q=${Uri.encode(query)}"
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

    fun handleResolutionSuccess(context: Context, item: com.blankdev.crossfade.data.HistoryItem) {
        val app = CrossfadeApp.instance
        val settings = app.settingsManager
        val resolver = app.linkResolver

        val isPodcast = PlatformRegistry.isPodcastUrl(item.originalUrl)
        val targetApp = if (isPodcast) SettingsManager.TARGET_PODCAST_WEB else settings.targetApp

        if (targetApp == SettingsManager.TARGET_UNIVERSAL) {
            showMenu(context, item)
        } else {
            val linksJson = item.linksJson
            if (!linksJson.isNullOrBlank() && linksJson != "{}") {
                try {
                    val linksType = object : TypeToken<Map<String, com.blankdev.crossfade.api.PlatformLink>>() {}.type
                    val linksByPlatform: Map<String, com.blankdev.crossfade.api.PlatformLink> = Gson().fromJson(linksJson, linksType)
                    
                    val response = com.blankdev.crossfade.api.OdesliResponse(
                        entityUniqueId = null,
                        userCountry = null,
                        pageUrl = item.pageUrl,
                        entitiesByUniqueId = null,
                        linksByPlatform = linksByPlatform
                    )
                    
                    val targetUrl = if (isPodcast) {
                        resolver.getPodcastTargetUrl(response, targetApp)
                    } else {
                        resolver.getTargetUrl(response, targetApp)
                    }

                    if (targetUrl != null) {
                        openUrl(context, targetUrl)
                    } else {
                        showMenu(context, item)
                    }
                } catch (e: Exception) {
                    showMenu(context, item)
                }
            } else {
                showMenu(context, item)
            }
        }
    }
}
