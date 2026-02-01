package com.blankdev.crossfade.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.blankdev.crossfade.CrossfadeApp
import com.blankdev.crossfade.R
import com.blankdev.crossfade.api.PlatformLink
import com.blankdev.crossfade.data.HistoryItem
import com.blankdev.crossfade.databinding.BottomSheetShareBinding
import com.blankdev.crossfade.utils.PlatformRegistry
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
        const val ID_FIX_MATCH = "fix_match"

        fun showResolveFlow(
            fragmentManager: androidx.fragment.app.FragmentManager,
            item: HistoryItem,
            onResolved: (HistoryItem) -> Unit
        ) {
            val searchSheet = ResolveSearchBottomSheet.newInstance(item, "Song")
            searchSheet.onResolved = onResolved
            searchSheet.show(fragmentManager, "resolve_search")
        }
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
        } else if (menuItem.id == ID_FIX_MATCH) {
            showResolveFlow(parentFragmentManager, currentItem) { updatedItem ->
                item = updatedItem
                setupMenu()
            }
        }
    }

    private fun buildMenuItems(item: HistoryItem, links: Map<String, PlatformLink>): List<MenuItemData> {
        val list = mutableListOf<MenuItemData>()
        val sourcePlatformId = PlatformRegistry.getPlatformFromUrl(item.originalUrl)
        val sourcePlatform = sourcePlatformId?.let { PlatformRegistry.getPlatformByInternalId(it) }

        if (item.isResolved) {
            // 1. Source URL
            list.add(MenuItemData(
                id = "source_url",
                title = "Source URL",
                url = item.originalUrl,
                type = TYPE_ITEM,
                iconResId = sourcePlatform?.iconResId ?: R.drawable.ic_placeholder_service
            ))

            // 2. Preferred App
            val targetApp = CrossfadeApp.instance.settingsManager.targetApp
            val targetPlatform = PlatformRegistry.getPlatformByInternalId(targetApp)
            
            var preferredUrl: String? = null
            if (targetApp == SettingsManager.TARGET_UNIVERSAL) {
                 // No single preferred app for Universal
            } else if (targetPlatform != null) {
                if (targetPlatform.odesliKey == "odesli") {
                    preferredUrl = item.pageUrl ?: "https://odesli.co/?q=${com.blankdev.crossfade.utils.LinkProcessor.openUrl(requireContext(), item.originalUrl)}"
                } else if (targetPlatform.odesliKey != null) {
                    preferredUrl = links[targetPlatform.odesliKey]?.url
                }
            }
            
            if (preferredUrl != null && targetPlatform != null) {
                list.add(MenuItemData(
                    id = "target_link",
                    title = targetPlatform.displayName,
                    url = preferredUrl,
                    type = TYPE_ITEM,
                    iconResId = targetPlatform.iconResId
                ))
            }

            // 3. Additional Apps
            val enabledServices = CrossfadeApp.instance.settingsManager.additionalServices
            val additionalItems = mutableListOf<MenuItemData>()
            
            links.forEach { (key, link) ->
                val platform = PlatformRegistry.getPlatformByOdesliKey(key)
                if (platform != null && platform.internalId != targetApp && platform.internalId != sourcePlatformId && enabledServices.contains(key) && link.url != null) {
                    additionalItems.add(MenuItemData(
                        id = "additional_$key",
                        title = platform.displayName,
                        url = link.url,
                        type = TYPE_ITEM,
                        iconResId = platform.iconResId
                    ))
                }
            }
            
            if (enabledServices.contains("odesli") && targetApp != "odesli" && sourcePlatformId != "odesli") {
                val odesliUrl = item.pageUrl ?: "https://odesli.co/?q=${android.net.Uri.encode(item.originalUrl)}"
                additionalItems.add(MenuItemData(
                    id = "additional_odesli",
                    title = "Odesli",
                    url = odesliUrl,
                    type = TYPE_ITEM,
                    iconResId = R.drawable.ic_odesli
                ))
            }

            if (additionalItems.isNotEmpty()) {
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

            if (additionalItems.isEmpty() && preferredUrl == null) {
                val searchQuery = if (!item.songTitle.isNullOrBlank()) {
                    if (!item.artistName.isNullOrBlank()) "${item.songTitle} ${item.artistName}" else item.songTitle
                } else {
                    item.originalUrl
                }

                list.add(MenuItemData(
                    id = "search_odesli_fallback",
                    title = "Search on Odesli...",
                    url = "https://odesli.co/?q=${android.net.Uri.encode(searchQuery)}",
                    type = TYPE_ITEM,
                    iconResId = R.drawable.ic_search
                ))
            }
        } else {
            list.add(MenuItemData(
                id = "source_url_unresolved",
                title = "Source URL",
                url = item.originalUrl,
                type = TYPE_ITEM,
                iconResId = sourcePlatform?.iconResId ?: R.drawable.ic_placeholder_service
            ))

            list.add(MenuItemData(
                id = "search_odesli",
                title = "Search on Odesli...",
                url = "https://odesli.co/?q=${android.net.Uri.encode(item.originalUrl)}",
                type = TYPE_ITEM,
                iconResId = R.drawable.ic_search
            ))
        }

        list.add(MenuItemData(
            id = ID_FIX_MATCH,
            title = if (item.isResolved) "Fix match" else "Resolve",
            type = TYPE_ACTION,
            isAction = true,
            iconResId = R.drawable.ic_search
        ))

        list.add(MenuItemData(
            id = ID_CLEAR_ITEM,
            title = "Clear item",
            type = TYPE_ACTION,
            isAction = true
        ))

        return list
    }
}
