package com.blankdev.crossfade.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.blankdev.crossfade.CrossfadeApp
import com.blankdev.crossfade.R
import com.blankdev.crossfade.databinding.ActivityAdditionalLinksBinding
import com.blankdev.crossfade.utils.SettingsManager
import androidx.activity.enableEdgeToEdge
import com.google.android.material.chip.Chip
import com.google.android.material.color.MaterialColors

class AdditionalLinksActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdditionalLinksBinding
    private val settings by lazy { CrossfadeApp.instance.settingsManager }
    
    // Map of platform IDs to Display Names
    private val platforms = mapOf(
        "spotify" to "Spotify",
        "tidal" to "Tidal",
        "youtube" to "YouTube",
        "youtubeMusic" to "YouTube Music",
        "amazonMusic" to "Amazon Music",
        "appleMusic" to "Apple Music",
        "soundcloud" to "SoundCloud",
        "bandcamp" to "Bandcamp",
        "deezer" to "Deezer",
        "napster" to "Napster",
        "pandora" to "Pandora",
        "anghami" to "Anghami",
        "yandex" to "Yandex Music",
        "audiomack" to "Audiomack",
        "audius" to "Audius",
        "boomplay" to "Boomplay",
        "odesli" to "Odesli"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityAdditionalLinksBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.topAppBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.topAppBar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        
        loadSettings()
    }

    private fun loadSettings() {
        val enabledServices = settings.additionalServices
        binding.chipGroupServices.removeAllViews()
        platforms.forEach { (id, name) ->
            val chip = Chip(this).apply {
                text = name
                isCheckable = true
                isChecked = enabledServices.contains(id)
                chipIcon = if (isChecked) androidx.core.content.ContextCompat.getDrawable(this@AdditionalLinksActivity, R.drawable.ic_check) else null
                isChipIconVisible = isChecked
                
                updateChipAppearance(this, isChecked)

                setOnCheckedChangeListener { _, checked ->
                    val currentSelection = settings.additionalServices.toMutableSet()
                    if (checked) {
                        currentSelection.add(id)
                    } else {
                        currentSelection.remove(id)
                    }
                    settings.additionalServices = currentSelection
                    
                    chipIcon = if (checked) androidx.core.content.ContextCompat.getDrawable(this@AdditionalLinksActivity, R.drawable.ic_check) else null
                    isChipIconVisible = checked
                    updateChipAppearance(this, checked)
                }
            }
            binding.chipGroupServices.addView(chip)
        }
    }

    private fun updateChipAppearance(chip: Chip, isChecked: Boolean) {
        try {
            val colorPrimaryContainer = MaterialColors.getColor(chip, com.google.android.material.R.attr.colorPrimaryContainer, Color.LTGRAY)
            val colorOnPrimaryContainer = MaterialColors.getColor(chip, com.google.android.material.R.attr.colorOnPrimaryContainer, Color.BLACK)
            val colorSurfaceVariant = MaterialColors.getColor(chip, com.google.android.material.R.attr.colorSurfaceVariant, Color.GRAY)
            val colorOnSurfaceVariant = MaterialColors.getColor(chip, com.google.android.material.R.attr.colorOnSurfaceVariant, Color.WHITE)

            if (isChecked) {
                chip.chipBackgroundColor = ColorStateList.valueOf(colorPrimaryContainer)
                chip.setTextColor(colorOnPrimaryContainer)
                chip.chipIconTint = ColorStateList.valueOf(colorOnPrimaryContainer)
            } else {
                chip.chipBackgroundColor = ColorStateList.valueOf(colorSurfaceVariant)
                chip.setTextColor(colorOnSurfaceVariant)
            }
        } catch (e: Exception) {
            chip.chipBackgroundColor = ColorStateList.valueOf(Color.LTGRAY)
        }
        chip.chipStrokeWidth = 0f
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
