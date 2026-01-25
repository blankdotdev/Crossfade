package com.blankdev.crossfade.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.blankdev.crossfade.CrossfadeApp
import com.blankdev.crossfade.databinding.ActivitySettingsBinding
import com.blankdev.crossfade.utils.SettingsManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val settings by lazy { CrossfadeApp.instance.settingsManager }
    private var isInitialLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
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

    override fun onResume() {
        super.onResume()
        loadSettings()
        checkThemeConsistency()
    }

    private fun checkThemeConsistency() {
        val expectedMode = when (settings.themeMode) {
            SettingsManager.THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            SettingsManager.THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        
        // 1. Ensure global default matches settings
        if (AppCompatDelegate.getDefaultNightMode() != expectedMode) {
            android.util.Log.d("SettingsActivity", "Updating global night mode to $expectedMode")
            AppCompatDelegate.setDefaultNightMode(expectedMode)
        }

        // 2. Check if current configuration matches expected mode (for explicit Light/Dark)
        val currentNightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        val isNightModeActive = currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES

        val output = when (expectedMode) {
            AppCompatDelegate.MODE_NIGHT_YES -> if (!isNightModeActive) "recreate" else "ok"
            AppCompatDelegate.MODE_NIGHT_NO -> if (isNightModeActive) "recreate" else "ok"
            else -> "ok" // System follows system, we assume it works
        }

        if (output == "recreate") {
            android.util.Log.d("SettingsActivity", "Recreating activity to apply theme")
            recreate()
        }
    }

    private fun loadSettings() {
        isInitialLoading = true
        try {
            // Theme
            val themeId = when (settings.themeMode) {
                SettingsManager.THEME_LIGHT -> binding.btnThemeLight.id
                SettingsManager.THEME_DARK -> binding.btnThemeDark.id
                else -> binding.btnThemeSystem.id
            }
            binding.toggleTheme.check(themeId)
        } finally {
            isInitialLoading = false
        }
    }

    private fun setupListeners() {
        binding.toggleTheme.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked && !isInitialLoading) {
                val themeValue = when (checkedId) {
                    binding.btnThemeLight.id -> SettingsManager.THEME_LIGHT
                    binding.btnThemeDark.id -> SettingsManager.THEME_DARK
                    else -> SettingsManager.THEME_SYSTEM
                }
                
                if (settings.themeMode != themeValue) {
                    settings.themeMode = themeValue
                    val mode = when (themeValue) {
                        SettingsManager.THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                        SettingsManager.THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
                        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    }
                    AppCompatDelegate.setDefaultNightMode(mode)
                }
            }
        }

        binding.btnPreferredApp.setOnClickListener {
            startActivity(Intent(this, PreferredAppActivity::class.java))
        }

        binding.btnAdditionalLinks.setOnClickListener {
            startActivity(Intent(this, AdditionalLinksActivity::class.java))
        }

        binding.btnBackupRestore.setOnClickListener {
            startActivity(Intent(this, BackupRestoreActivity::class.java))
        }
        
        binding.btnSetDefault.setOnClickListener {
            openSystemSettings()
        }

        binding.btnCheckDefaults.setOnClickListener {
            DefaultsCheckBottomSheet().show(supportFragmentManager, "DefaultsCheck")
        }

        binding.btnAbout.setOnClickListener {
            AboutBottomSheet().show(supportFragmentManager, "About")
        }
    }

    private fun openSystemSettings() {
        try {
            val intent = Intent(android.provider.Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS)
            intent.data = android.net.Uri.parse("package:$packageName")
            startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = android.net.Uri.parse("package:$packageName")
                startActivity(intent)
            } catch (e: Exception) {
                android.widget.Toast.makeText(this, "Could not open settings", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
}
