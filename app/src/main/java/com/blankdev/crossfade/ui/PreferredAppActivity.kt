package com.blankdev.crossfade.ui

import android.os.Bundle
import android.widget.RadioButton
import androidx.appcompat.app.AppCompatActivity
import com.blankdev.crossfade.CrossfadeApp
import com.blankdev.crossfade.databinding.ActivityPreferredAppBinding
import com.blankdev.crossfade.utils.SettingsManager
import androidx.activity.enableEdgeToEdge

class PreferredAppActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPreferredAppBinding
    private val settings by lazy { CrossfadeApp.instance.settingsManager }
    private var isInitialLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityPreferredAppBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.topAppBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.topAppBar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        
        loadSettings()
        setupListeners()
    }

    private fun loadSettings() {
        isInitialLoading = true
        try {
            when (settings.targetApp) {
                SettingsManager.TARGET_SPOTIFY -> binding.radioSpotify.isChecked = true
                SettingsManager.TARGET_APPLE_MUSIC -> binding.radioApple.isChecked = true
                SettingsManager.TARGET_TIDAL -> binding.radioTidal.isChecked = true
                SettingsManager.TARGET_AMAZON_MUSIC -> binding.radioAmazon.isChecked = true
                SettingsManager.TARGET_YOUTUBE_MUSIC -> binding.radioYoutube.isChecked = true
                SettingsManager.TARGET_UNIVERSAL -> binding.radioUniversal.isChecked = true
                SettingsManager.PLATFORM_DEEZER -> binding.radioDeezer.isChecked = true
                SettingsManager.PLATFORM_SOUNDCLOUD -> binding.radioSoundcloud.isChecked = true
                SettingsManager.PLATFORM_NAPSTER -> binding.radioNapster.isChecked = true
                SettingsManager.PLATFORM_PANDORA -> binding.radioPandora.isChecked = true
                SettingsManager.PLATFORM_AUDIOMACK -> binding.radioAudiomack.isChecked = true
                SettingsManager.PLATFORM_ANGHAMI -> binding.radioAnghami.isChecked = true
                SettingsManager.PLATFORM_BOOMPLAY -> binding.radioBoomplay.isChecked = true
                SettingsManager.PLATFORM_YANDEX -> binding.radioYandex.isChecked = true
                SettingsManager.PLATFORM_AUDIUS -> binding.radioAudius.isChecked = true
                SettingsManager.PLATFORM_BANDCAMP -> binding.radioBandcamp.isChecked = true
                SettingsManager.PLATFORM_YOUTUBE -> binding.radioYoutubePlain.isChecked = true
            }
        } finally {
            isInitialLoading = false
        }
    }

    private fun setupListeners() {
        binding.radioGroupTarget.setOnCheckedChangeListener { _, checkedId ->
            if (!isInitialLoading) {
                val target = when (checkedId) {
                    binding.radioSpotify.id -> SettingsManager.TARGET_SPOTIFY
                    binding.radioApple.id -> SettingsManager.TARGET_APPLE_MUSIC
                    binding.radioTidal.id -> SettingsManager.TARGET_TIDAL
                    binding.radioAmazon.id -> SettingsManager.TARGET_AMAZON_MUSIC
                    binding.radioYoutube.id -> SettingsManager.TARGET_YOUTUBE_MUSIC
                    binding.radioUniversal.id -> SettingsManager.TARGET_UNIVERSAL
                    binding.radioDeezer.id -> SettingsManager.PLATFORM_DEEZER
                    binding.radioSoundcloud.id -> SettingsManager.PLATFORM_SOUNDCLOUD
                    binding.radioNapster.id -> SettingsManager.PLATFORM_NAPSTER
                    binding.radioPandora.id -> SettingsManager.PLATFORM_PANDORA
                    binding.radioAudiomack.id -> SettingsManager.PLATFORM_AUDIOMACK
                    binding.radioAnghami.id -> SettingsManager.PLATFORM_ANGHAMI
                    binding.radioBoomplay.id -> SettingsManager.PLATFORM_BOOMPLAY
                    binding.radioYandex.id -> SettingsManager.PLATFORM_YANDEX
                    binding.radioAudius.id -> SettingsManager.PLATFORM_AUDIUS
                    binding.radioBandcamp.id -> SettingsManager.PLATFORM_BANDCAMP
                    binding.radioYoutubePlain.id -> SettingsManager.PLATFORM_YOUTUBE
                    else -> SettingsManager.TARGET_SPOTIFY
                }
                
                // Check for conflict before saving
                if (com.blankdev.crossfade.utils.DefaultHandlerChecker.hasConflict(this, target)) {
                    showConflictWarning(target)
                } else {
                    settings.targetApp = target
                }
            }
        }
    }
    
    private fun showConflictWarning(target: String) {
        val platformName = com.blankdev.crossfade.utils.DefaultHandlerChecker.getPlatformDisplayName(target)
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Infinite Loop Detected")
            .setMessage("Crossfade is currently set as the default handler for $platformName links. " +
                    "This will cause an infinite loop where links keep opening in Crossfade.\n\n" +
                    "What would you like to do?")
            .setPositiveButton("Change Default Handler") { _, _ ->
                // Revert selection
                loadSettings()
                // Open system settings
                openSystemSettings()
            }
            .setNeutralButton("Use Odesli Instead") { _, _ ->
                // Save the target but mark to use Odesli for conflicts
                settings.targetApp = target
                settings.useOdesliForConflicts = true
                android.widget.Toast.makeText(this, "Links will open in Odesli to avoid conflicts", android.widget.Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Cancel") { _, _ ->
                // Revert selection
                loadSettings()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun openSystemSettings() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val intent = android.content.Intent(android.provider.Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS)
                intent.data = android.net.Uri.parse("package:$packageName")
                startActivity(intent)
            } else {
                openApplicationDetailsSub()
            }
        } catch (e: Exception) {
            openApplicationDetailsSub()
        }
    }

    private fun openApplicationDetailsSub() {
        try {
            val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = android.net.Uri.parse("package:$packageName")
            startActivity(intent)
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "Could not open settings", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    override fun onResume() {
        super.onResume()
        checkThemeConsistency()
    }
    
    private fun checkThemeConsistency() {
        val settings = CrossfadeApp.instance.settingsManager
        val expectedMode = when (settings.themeMode) {
            SettingsManager.THEME_LIGHT -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
            SettingsManager.THEME_DARK -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
            else -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        
        if (androidx.appcompat.app.AppCompatDelegate.getDefaultNightMode() != expectedMode) {
             androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(expectedMode)
        }

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
}
