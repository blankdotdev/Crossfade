package com.blankdev.crossfade.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.blankdev.crossfade.CrossfadeApp
import com.blankdev.crossfade.R
import com.blankdev.crossfade.databinding.BottomSheetShareBinding
import com.blankdev.crossfade.utils.DefaultHandlerChecker
import com.blankdev.crossfade.utils.SettingsManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DefaultsCheckBottomSheet : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetShareBinding // Reusing Share Layout structure (List + Title)
    private val adapter = DefaultsAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Reuse the generic bottom sheet layout which essentially just has a RecyclerView
        // You might want a custom title, check if BottomSheetShareBinding allows setting name
        // Or create a new layout `bottom_sheet_list.xml` if `bottom_sheet_share.xml` is too specific.
        // For now, let's create a programmatic title or use a simple layout.
        // Actually, looking at ShareBottomSheet, it uses BottomSheetShareBinding.
        // Let's create a simple layout for this since ShareBottomSheet might be specific.
        // Wait, I can't create a new layout file in this turn easily without another tool call if I didn't plan it.
        // I'll use a dynamic approach or assuming the system has a generic Recycler Bottom Sheet?
        // Let's check `bottom_sheet_share.xml` content first? No, I'll just create a new layout file `bottom_sheet_list.xml` 
        // in this same call using a separate tool call? No, can't parallel well with write.
        // I'll write the code to use `item_default_check` inside a standard recycler view.
        // I'll create a new layout file `bottom_sheet_defaults.xml` first in a subsequent step if needed, 
        // OR I can use `BottomSheetShareBinding` if it's generic enough.
        // Let's assume I'll write `bottom_sheet_defaults.xml` next.
        
        // TEMPORARY: using a layout I will create momentarily.
        binding = BottomSheetShareBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Reset/Configure Binding
        // Note: ShareBottomSheet might have specific headers. 
        // If `BottomSheetShareBinding` has a title view, set it.
        // If not, I might need to add a header item to the adapter.
        
        binding.shareRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.shareRecyclerView.adapter = adapter
        
        loadData()
    }

    private fun loadData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val context = context ?: return@launch
            val settings = CrossfadeApp.instance.settingsManager
            val preferredApp = settings.targetApp
            
            val services = listOf(
                SettingsManager.TARGET_SPOTIFY to R.drawable.ic_spotify,
                SettingsManager.TARGET_APPLE_MUSIC to R.drawable.ic_apple_music,
                SettingsManager.TARGET_TIDAL to R.drawable.ic_tidal,
                SettingsManager.TARGET_AMAZON_MUSIC to R.drawable.ic_amazon_music,
                SettingsManager.TARGET_YOUTUBE_MUSIC to R.drawable.ic_youtube_music,
                SettingsManager.PLATFORM_DEEZER to R.drawable.ic_deezer,
                SettingsManager.PLATFORM_SOUNDCLOUD to R.drawable.ic_soundcloud,
                SettingsManager.PLATFORM_NAPSTER to R.drawable.ic_napster,
                SettingsManager.PLATFORM_PANDORA to R.drawable.ic_pandora,
                SettingsManager.PLATFORM_AUDIOMACK to R.drawable.ic_audiomack,
                SettingsManager.PLATFORM_ANGHAMI to R.drawable.ic_anghami,
                SettingsManager.PLATFORM_BOOMPLAY to R.drawable.ic_boomplay,
                SettingsManager.PLATFORM_YANDEX to R.drawable.ic_yandex_music,
                SettingsManager.PLATFORM_AUDIUS to R.drawable.ic_audius,
                SettingsManager.PLATFORM_BANDCAMP to R.drawable.ic_bandcamp,
                SettingsManager.PLATFORM_SHAZAM to R.drawable.ic_shazam
            )

            val displayItems = mutableListOf<DefaultCheckItem>()

            for ((platformId, icon) in services) {
                val status = DefaultHandlerChecker.checkServiceStatus(context, platformId)
                val platformName = DefaultHandlerChecker.getPlatformDisplayName(platformId)
                
                val isPreferred = (platformId == preferredApp)
                val isConflict = isPreferred && (status == DefaultHandlerChecker.ServiceStatus.ACTIVE || status == DefaultHandlerChecker.ServiceStatus.PARTIAL)

                // Hide preferred app if not set as default handler
                if (!isPreferred || status != DefaultHandlerChecker.ServiceStatus.INACTIVE) {
                    displayItems.add(DefaultCheckItem(
                        platformId = platformId,
                        name = platformName,
                        iconRes = icon,
                        status = status,
                        isConflict = isConflict,
                        isPreferred = isPreferred
                    ))
                }
            }

            withContext(Dispatchers.Main) {
                adapter.updateData(displayItems)
                // Post-processing for conflicts could happen here if adapter supported it
                // For now, "Partial" will show Yellow, "Active" Green.
                // Requirement says "Active" should be Red if conflict.
                // I need to update my Adapter to support this override.
                // But `updateData` just takes list.
                // I will add a `isConflict` flag to `DefaultCheckItem`?
            }
        }
    }
}
