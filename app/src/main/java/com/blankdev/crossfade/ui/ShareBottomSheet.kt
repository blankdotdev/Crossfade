package com.blankdev.crossfade.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankdev.crossfade.CrossfadeApp
import com.blankdev.crossfade.R
import com.blankdev.crossfade.api.PlatformLink
import com.blankdev.crossfade.data.HistoryItem
import com.blankdev.crossfade.databinding.BottomSheetShareBinding
import com.blankdev.crossfade.utils.SettingsManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch

class ShareBottomSheet : BottomSheetDialogFragment() {
    
    private var item: HistoryItem? = null

    companion object {
        private const val ARG_ITEM_JSON = "arg_item_json"
        
        fun newInstance(item: HistoryItem): ShareBottomSheet {
            val fragment = ShareBottomSheet()
            val args = Bundle()
            args.putString(ARG_ITEM_JSON, Gson().toJson(item))
            fragment.arguments = args
            return fragment
        }
        
        const val TYPE_ITEM = 0
        const val TYPE_SUBMENU_HEADER = 1
        const val TYPE_SUBMENU_ITEM = 2
        const val TYPE_ACTION = 3
        
        const val ID_CLEAR_ITEM = "clear_item"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val json = arguments?.getString(ARG_ITEM_JSON)
        if (json != null) {
            item = Gson().fromJson(json, HistoryItem::class.java)
        }
    }

    private lateinit var binding: BottomSheetShareBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetShareBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupMenu()
    }

    private fun setupMenu() {
        val currentItem = item ?: return
        val linksJson = currentItem.linksJson
        val rawLinks: Map<String, PlatformLink> = if (!linksJson.isNullOrEmpty()) {
            try {
                val type = object : TypeToken<Map<String, PlatformLink>>() {}.type
                Gson().fromJson(linksJson, type)
            } catch (e: Exception) {
                emptyMap()
            }
        } else {
            emptyMap()
        }

        // Merge iTunes into Apple Music
        val links = com.blankdev.crossfade.utils.LinkUtils.mergeAppleMusicLinks(rawLinks)

        val menuItems = buildMenuItems(currentItem, links)
        
        binding.shareRecyclerView.layoutManager = LinearLayoutManager(context)
        val adapter = ItemMenuAdapter(
            onItemClick = { menuItem ->
                if (menuItem.isAction) {
                    handleAction(menuItem)
                } else if (menuItem.url != null) {
                    context?.let { ctx ->
                        com.blankdev.crossfade.utils.LinkProcessor.openUrl(ctx, menuItem.url)
                        dismiss()
                    }
                }
            },
            onCopyClick = { url ->
                context?.let { ctx ->
                    val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("URL", url)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(ctx, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                }
            },
            onShareClick = { url ->
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, url)
                }
                startActivity(Intent.createChooser(shareIntent, "Share link"))
                dismiss()
            }
        )
        binding.shareRecyclerView.adapter = adapter
        
        // Auto-expand "More..." if "Ask everytime" (Universal) is the target
        val targetApp = CrossfadeApp.instance.settingsManager.targetApp
        if (targetApp == SettingsManager.TARGET_UNIVERSAL) {
            menuItems.find { it.id == "header_more" }?.isExpanded = true
        }
        
        adapter.setData(menuItems)
    }

    private fun handleAction(menuItem: MenuItemData) {
        val currentItem = item ?: return
        if (menuItem.id == ID_CLEAR_ITEM) {
            context?.let { ctx ->
                MaterialAlertDialogBuilder(ctx)
                    .setTitle("Delete Item")
                    .setMessage("Are you sure you want to remove this item from history?")
                    .setPositiveButton("Delete") { _, _ ->
                         lifecycleScope.launch {
                            CrossfadeApp.instance.database.historyDao().delete(currentItem)
                            dismiss()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    private fun buildMenuItems(item: HistoryItem, links: Map<String, PlatformLink>): List<MenuItemData> {
        val list = mutableListOf<MenuItemData>()
        val sourcePlatform = getPlatformFromUrl(item.originalUrl)

        if (item.isResolved) {
            // 1. Source URL
            list.add(MenuItemData(
                id = "source_url",
                title = "Source URL",
                url = item.originalUrl,
                type = TYPE_ITEM,
                iconResId = getIconForPlatform(sourcePlatform ?: "")
            ))

            // 2. Preferred App
            val targetApp = CrossfadeApp.instance.settingsManager.targetApp
            val targetPlatformKey = getPlatformKeyForTarget(targetApp)
            
            // Find preferred link
            var preferredUrl: String? = null
            if (targetPlatformKey == "odesli") {
                preferredUrl = item.pageUrl ?: "https://odesli.co/?q=${Uri.encode(item.originalUrl)}"
            } else if (targetPlatformKey != null) {
                preferredUrl = links[targetPlatformKey]?.url
            }
            
            if (preferredUrl != null) {
                list.add(MenuItemData(
                    id = "target_link",
                    title = getPrettyPlatformName(targetPlatformKey!!),
                    url = preferredUrl,
                    type = TYPE_ITEM,
                    iconResId = getIconForPlatform(targetPlatformKey)
                ))
            }

            // 3. Additional Apps
            val enabledServices = CrossfadeApp.instance.settingsManager.additionalServices
            
            // Collect all potential additional links
            val additionalItems = mutableListOf<MenuItemData>()
            
            // From regular platform links
            links.filter { (key, link) ->
                // Filter out the target app (already shown) AND the source platform (already shown as source)
                key != targetPlatformKey && key != sourcePlatform && enabledServices.contains(key) && link.url != null
            }.forEach { (key, link) ->
                additionalItems.add(MenuItemData(
                    id = "additional_$key",
                    title = getPrettyPlatformName(key),
                    url = link.url,
                    type = TYPE_ITEM,
                    iconResId = getIconForPlatform(key)
                ))
            }
            
            // From Odesli (if enabled and not target/source)
            // ONLY if links are not empty. If links are empty, we'll show Search Fallback instead.
            if (links.isNotEmpty() && enabledServices.contains("odesli") && targetPlatformKey != "odesli" && sourcePlatform != "odesli") {
                val odesliUrl = item.pageUrl ?: "https://odesli.co/?q=${Uri.encode(item.originalUrl)}"
                additionalItems.add(MenuItemData(
                    id = "additional_odesli",
                    title = "Odesli",
                    url = odesliUrl,
                    type = TYPE_ITEM,
                    iconResId = R.drawable.ic_odesli
                ))
            }

            if (additionalItems.isNotEmpty()) {
                // If it's a "Ask everytime" target, we ALWAYS want it under "More..." to support auto-expansion
                val isAskEverytime = targetApp == SettingsManager.TARGET_UNIVERSAL
                
                if (additionalItems.size == 1 && !isAskEverytime) {
                    list.add(additionalItems[0])
                } else {
                    val subItems = additionalItems.map { it.copy(type = TYPE_SUBMENU_ITEM) }
                    list.add(MenuItemData(
                        id = "header_more",
                        title = "More...",
                        type = TYPE_SUBMENU_HEADER,
                        subItems = subItems
                    ))
                }
            }

            // 4. Search on Odesli... (Only if no platform links were found - the "scraped but unresolved" case)
            if (additionalItems.isEmpty() && preferredUrl == null) {
                val searchQuery = if (!item.songTitle.isNullOrBlank()) {
                    if (!item.artistName.isNullOrBlank()) "${item.songTitle} ${item.artistName}" else item.songTitle
                } else {
                    item.originalUrl
                }

                list.add(MenuItemData(
                    id = "search_odesli_fallback",
                    title = "Search on Odesli...",
                    url = "https://odesli.co/?q=${Uri.encode(searchQuery)}",
                    type = TYPE_ITEM,
                    iconResId = R.drawable.ic_search
                ))
            }
        } else {
            // Logic for TRULY UNRESOLVED items
            // 1. Source URL
            list.add(MenuItemData(
                id = "source_url_unresolved",
                title = "Source URL",
                url = item.originalUrl,
                type = TYPE_ITEM,
                iconResId = getIconForPlatform(sourcePlatform ?: "")
            ))

            // 2. Search on Odesli...
            list.add(MenuItemData(
                id = "search_odesli",
                title = "Search on Odesli...",
                url = "https://odesli.co/?q=${Uri.encode(item.originalUrl)}",
                type = TYPE_ITEM,
                iconResId = R.drawable.ic_search
            ))
        }

        // 4. Clear Item
        list.add(MenuItemData(
            id = ID_CLEAR_ITEM,
            title = "Clear item",
            type = TYPE_ACTION,
            isAction = true
        ))

        return list
    }
    
    // ... helper methods ...
    private fun getPlatformKeyForTarget(target: String): String? {
        return when (target) {
            SettingsManager.TARGET_SPOTIFY -> "spotify"
            SettingsManager.TARGET_APPLE_MUSIC -> "appleMusic"
            SettingsManager.TARGET_TIDAL -> "tidal"
            SettingsManager.TARGET_AMAZON_MUSIC -> "amazonMusic"
            SettingsManager.TARGET_YOUTUBE_MUSIC -> "youtubeMusic"
            // For "Ask everytime", we return null so it's treated as additional services (moving it to "More...")
            SettingsManager.TARGET_UNIVERSAL -> null
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
            else -> null
        }
    }

    private fun getPrettyPlatformName(key: String): String {
        return when(key) {
            "spotify" -> "Spotify"
            "appleMusic" -> "Apple Music"
            "youtubeMusic" -> "YouTube Music"
            "youtube" -> "YouTube"
            "tidal" -> "Tidal"
            "amazonMusic" -> "Amazon Music"
            "deezer" -> "Deezer"
            "soundcloud" -> "SoundCloud"
            "napster" -> "Napster"
            "pandora" -> "Pandora"
            "audiomack" -> "Audiomack"
            "shazam" -> "Shazam"
            "yandex" -> "Yandex Music"
            "anghami" -> "Anghami"
            "boomplay" -> "Boomplay"
            "bandcamp" -> "Bandcamp"
            "audius" -> "Audius"
            "odesli" -> "Odesli"
            else -> key.replaceFirstChar { it.uppercase() }
        }
    }

    private fun getIconForPlatform(key: String): Int? {
        return when(key) {
             "spotify" -> R.drawable.ic_spotify
             "appleMusic" -> R.drawable.ic_apple_music
             "youtubeMusic" -> R.drawable.ic_youtube_music
             "tidal" -> R.drawable.ic_tidal
             "amazonMusic" -> R.drawable.ic_amazon_music
             "shazam" -> R.drawable.ic_shazam
             "soundcloud" -> R.drawable.ic_soundcloud
             "youtube" -> R.drawable.ic_youtube
             "deezer" -> R.drawable.ic_deezer
             "napster" -> R.drawable.ic_napster
             "pandora" -> R.drawable.ic_pandora
             "audiomack" -> R.drawable.ic_audiomack
             "yandex" -> R.drawable.ic_yandex_music
             "bandcamp" -> R.drawable.ic_bandcamp
             "anghami" -> R.drawable.ic_anghami
             "boomplay" -> R.drawable.ic_boomplay
             "audius" -> R.drawable.ic_audius
             "odesli" -> R.drawable.ic_odesli
             else -> R.drawable.ic_placeholder_service
        }
    }

    private fun getPlatformFromUrl(url: String): String? {
        return when {
            url.contains("spotify.com") -> "spotify"
            url.contains("apple.com") || url.contains("itunes.apple.com") -> "appleMusic"
            url.contains("music.youtube.com") -> "youtubeMusic"
            url.contains("youtube.com") || url.contains("youtu.be") -> "youtube"
            url.contains("tidal.com") -> "tidal"
            url.contains("amazon.com") -> "amazonMusic"
            url.contains("shazam.com") -> "shazam"
            url.contains("soundcloud.com") -> "soundcloud"
            url.contains("deezer.com") -> "deezer"
            url.contains("napster.com") -> "napster"
            url.contains("pandora.com") -> "pandora"
            url.contains("audiomack.com") -> "audiomack"
            url.contains("yandex.com") || url.contains("yandex.ru") -> "yandex"
            url.contains("anghami.com") -> "anghami"
            url.contains("boomplay.com") -> "boomplay"
            url.contains("bandcamp.com") -> "bandcamp"
            url.contains("odesli.co") || url.contains("song.link") || url.contains("album.link") -> "odesli"
            else -> null
        }
    }


}
