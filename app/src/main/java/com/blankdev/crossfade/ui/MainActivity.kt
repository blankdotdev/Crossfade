package com.blankdev.crossfade.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.blankdev.crossfade.CrossfadeApp
import com.blankdev.crossfade.utils.SettingsManager
import com.blankdev.crossfade.R
import android.widget.PopupMenu
import com.google.gson.Gson
import com.blankdev.crossfade.api.PlatformLink
import com.google.gson.reflect.TypeToken
import com.blankdev.crossfade.data.HistoryItem
import com.blankdev.crossfade.data.ResolveResult
import com.blankdev.crossfade.databinding.ActivityMainBinding
import androidx.recyclerview.widget.ConcatAdapter
import kotlinx.coroutines.launch
import androidx.activity.enableEdgeToEdge

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val historyAdapter = HistoryAdapter(
        onMenuClick = { view, item -> showItemMenu(view, item) },
        onItemClick = { item -> 
             processUrl(item.originalUrl, forceNavigate = true, historyItem = item)
        }
    )
    private val unresolvedAdapter = UnresolvedAdapter(
        onMenuClick = { view, item -> showItemMenu(view, item) },
        onItemClick = { item -> 
             processUrl(item.originalUrl, forceNavigate = true, historyItem = item)
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        
        val settings = CrossfadeApp.instance.settingsManager
        if (!settings.hasCompletedOnboarding) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        // Check and schedule automatic backup if needed
        checkAndScheduleBackup()

        setupRecyclerView()
        setupFab()
        handleIntent(intent)

        // Observe History
        lifecycleScope.launch {
            CrossfadeApp.instance.database.historyDao().getAllHistory().collect { list ->
                val resolved = list.filter { it.isResolved }
                val unresolved = list.filter { !it.isResolved }
                
                historyAdapter.submitList(resolved)
                unresolvedAdapter.submitList(unresolved)
                
                historyCount = list.size
                updateEmptyState(historyCount == 0)
                invalidateOptionsMenu()
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun setupRecyclerView() {
        val concatAdapter = ConcatAdapter(historyAdapter, unresolvedAdapter)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity).apply {
                // Enable item prefetching for smoother scrolling
                isItemPrefetchEnabled = true
                initialPrefetchItemCount = 4
            }
            adapter = concatAdapter
            
            // Performance optimizations
            setHasFixedSize(false) // Changed to false because unresolved section can change size
            setItemViewCacheSize(20) // Cache more views for smoother scrolling
            
            // Optimize item animations
            itemAnimator?.apply {
                changeDuration = 0 // Disable change animations for better performance
                addDuration = 150
                removeDuration = 150
            }
            
            // Enable nested scrolling optimization
            isNestedScrollingEnabled = true
        }
    }

    private fun setupFab() {
        binding.fab.setOnClickListener {
            showPasteUrlDialog()
        }
    }

    private fun showPasteUrlDialog() {
        AddLinkBottomSheet { url ->
            processUrl(url, forceNavigate = true)
        }.show(supportFragmentManager, "AddLink")
    }
    
    private var historyCount = 0

    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }
    
    override fun onPrepareOptionsMenu(menu: android.view.Menu?): Boolean {
        menu?.findItem(R.id.action_clear_all)?.isVisible = historyCount > 0
        return super.onPrepareOptionsMenu(menu)
    }
    
    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.action_clear_all -> {
                com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setTitle("Clear All History?")
                    .setMessage("Are you sure you want to clear all items from your history? This action cannot be undone.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Clear") { _, _ ->
                        lifecycleScope.launch {
                            CrossfadeApp.instance.database.historyDao().clearHistory()
                        }
                    }
                    .show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun handleIntent(intent: Intent?) {
        // MainActivity only handles non-VIEW/SEND intents (e.g., from launcher)
        // External links are now handled by RedirectActivity
        
        if (intent?.action == ACTION_FIX_MATCH) {
            val itemJson = intent.getStringExtra(EXTRA_ITEM_JSON)
            val url = intent.getStringExtra(EXTRA_URL)
            
            if (itemJson != null) {
                try {
                    val item = Gson().fromJson(itemJson, HistoryItem::class.java)
                    ShareBottomSheet.showResolveFlow(supportFragmentManager, item) { updatedItem ->
                        com.blankdev.crossfade.utils.LinkProcessor.handleResolutionSuccess(this, updatedItem)
                        finish()
                    }
                    return
                } catch (e: Exception) {
                    // Fallback to URL lookup
                }
            }
            
            if (url != null) {
                lifecycleScope.launch {
                    val item = CrossfadeApp.instance.database.historyDao().getHistoryItemByUrl(url)
                    if (item != null) {
                        ShareBottomSheet.showResolveFlow(supportFragmentManager, item) { updatedItem ->
                            com.blankdev.crossfade.utils.LinkProcessor.handleResolutionSuccess(this@MainActivity, updatedItem)
                            finish()
                        }
                    } else {
                        Toast.makeText(this@MainActivity, "Item not found in history", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun processUrl(url: String, forceNavigate: Boolean = false, historyItem: HistoryItem? = null) {
        com.blankdev.crossfade.utils.LinkProcessor.processUrl(this, lifecycleScope, url, forceNavigate, historyItem)
    }

    private fun showItemMenu(view: android.view.View, item: HistoryItem) {
        val sheet = ShareBottomSheet.newInstance(item)
        sheet.show(supportFragmentManager, "ShareSheet")
    }

    private fun showShareSheet(item: HistoryItem) {
         // ensuring this directs to the same sheet if called from elsewhere
         showItemMenu(binding.root, item)
    }

    private fun checkAndScheduleBackup() {
        val settings = CrossfadeApp.instance.settingsManager
        
        // Only proceed if auto-backup is enabled
        if (!settings.autoBackupEnabled) {
            return
        }

        // Check if backup folder is configured
        if (settings.autoBackupFolderUri.isNullOrBlank()) {
            return
        }

        // Check if backup is due based on frequency
        val lastBackup = settings.lastBackupTimestamp
        val now = System.currentTimeMillis()
        val timeSinceLastBackup = now - lastBackup

        val shouldBackup = when (settings.autoBackupFrequency) {
            SettingsManager.BACKUP_FREQUENCY_DAILY -> {
                timeSinceLastBackup >= 24 * 60 * 60 * 1000 // 24 hours
            }
            SettingsManager.BACKUP_FREQUENCY_WEEKLY -> {
                timeSinceLastBackup >= 7 * 24 * 60 * 60 * 1000 // 7 days
            }
            SettingsManager.BACKUP_FREQUENCY_MONTHLY -> {
                timeSinceLastBackup >= 30 * 24 * 60 * 60 * 1000 // 30 days
            }
            else -> false
        }

        if (shouldBackup) {
            // Schedule backup using WorkManager
            val workRequest = androidx.work.OneTimeWorkRequestBuilder<com.blankdev.crossfade.workers.BackupWorker>()
                .build()
            
            androidx.work.WorkManager.getInstance(this)
                .enqueueUniqueWork(
                    "auto_backup",
                    androidx.work.ExistingWorkPolicy.KEEP,
                    workRequest
                )
        }
    }
    override fun onResume() {
        super.onResume()
        checkThemeConsistency()
        updateEmptyState(historyCount == 0)
    }
    
    private fun checkThemeConsistency() {
        val settings = CrossfadeApp.instance.settingsManager
        val expectedMode = when (settings.themeMode) {
            SettingsManager.THEME_LIGHT -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
            SettingsManager.THEME_DARK -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
            else -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        
        // 1. Ensure global default matches settings
        if (androidx.appcompat.app.AppCompatDelegate.getDefaultNightMode() != expectedMode) {
             androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(expectedMode)
        }

        // 2. Check if current configuration matches expected mode
        val currentNightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        val isNightModeActive = currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES

        val output = when (expectedMode) {
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES -> if (!isNightModeActive) "recreate" else "ok"
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO -> if (isNightModeActive) "recreate" else "ok"
            else -> "ok"
        }

        if (output == "recreate") {
            recreate()
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyState.emptyStateContainer.isVisible = isEmpty
        binding.recyclerView.isVisible = !isEmpty
        
        if (isEmpty) {
            val settings = CrossfadeApp.instance.settingsManager
            val targetApp = settings.targetApp
            val excludePlatform = if (targetApp == SettingsManager.TARGET_UNIVERSAL) null else targetApp
            
            val handledCount = com.blankdev.crossfade.utils.DefaultHandlerChecker.getHandledLinksCount(this, excludePlatform)
            val totalCount = com.blankdev.crossfade.utils.DefaultHandlerChecker.getTotalRelevantLinksCount(excludePlatform)
            
            binding.emptyState.emptyStateText.text = getString(
                R.string.empty_state_message, 
                handledCount, 
                totalCount
            )
            
            if (targetApp == SettingsManager.TARGET_UNIVERSAL) {
                binding.emptyState.iconArrow2.isVisible = false
                binding.emptyState.iconTarget.isVisible = false
            } else {
                binding.emptyState.iconArrow2.isVisible = true
                binding.emptyState.iconTarget.isVisible = true
                
                val iconRes = when(targetApp) {
                    SettingsManager.TARGET_SPOTIFY -> R.drawable.ic_spotify
                    SettingsManager.TARGET_APPLE_MUSIC -> R.drawable.ic_apple_music
                    SettingsManager.TARGET_YOUTUBE_MUSIC -> R.drawable.ic_youtube_music
                    SettingsManager.TARGET_AMAZON_MUSIC -> R.drawable.ic_amazon_music
                    SettingsManager.TARGET_TIDAL -> R.drawable.ic_tidal
                    SettingsManager.PLATFORM_DEEZER -> R.drawable.ic_deezer
                    SettingsManager.PLATFORM_SOUNDCLOUD -> R.drawable.ic_soundcloud
                    SettingsManager.PLATFORM_NAPSTER -> R.drawable.ic_napster
                    SettingsManager.PLATFORM_PANDORA -> R.drawable.ic_pandora
                    SettingsManager.PLATFORM_AUDIOMACK -> R.drawable.ic_audiomack
                    SettingsManager.PLATFORM_ANGHAMI -> R.drawable.ic_anghami
                    SettingsManager.PLATFORM_BOOMPLAY -> R.drawable.ic_boomplay
                    SettingsManager.PLATFORM_YANDEX -> R.drawable.ic_yandex_music
                    SettingsManager.PLATFORM_AUDIUS -> R.drawable.ic_audius
                    SettingsManager.PLATFORM_BANDCAMP -> R.drawable.ic_bandcamp
                    SettingsManager.PLATFORM_SHAZAM -> R.drawable.ic_shazam
                    SettingsManager.PLATFORM_YOUTUBE -> R.drawable.ic_youtube
                    else -> R.drawable.ic_spotify
                }
                binding.emptyState.iconTarget.setImageResource(iconRes)
            }
        }
    }

    companion object {
        const val ACTION_FIX_MATCH = "com.blankdev.crossfade.ACTION_FIX_MATCH"
        const val EXTRA_URL = "com.blankdev.crossfade.EXTRA_URL"
        const val EXTRA_ITEM_JSON = "com.blankdev.crossfade.EXTRA_ITEM_JSON"
    }
}
