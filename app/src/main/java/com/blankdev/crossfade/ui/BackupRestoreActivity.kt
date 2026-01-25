package com.blankdev.crossfade.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.blankdev.crossfade.CrossfadeApp
import com.blankdev.crossfade.databinding.ActivityBackupRestoreBinding
import com.blankdev.crossfade.utils.BackupManager
import com.blankdev.crossfade.utils.SettingsManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

class BackupRestoreActivity : AppCompatActivity() {


    private lateinit var binding: ActivityBackupRestoreBinding
    private val settings by lazy { CrossfadeApp.instance.settingsManager }
    private val historyDao by lazy { CrossfadeApp.instance.database.historyDao() }
    private var isInitialLoading = false

    // Folder picker for manual/auto backup
    private val folderPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let { handleFolderSelected(it) }
    }

    // File picker for restore
    private val filePicker = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { handleRestoreFile(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityBackupRestoreBinding.inflate(layoutInflater)
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
            // Auto-backup toggle
            binding.switchAutoBackup.isChecked = settings.autoBackupEnabled
            updateAutoBackupVisibility()

            // Frequency selection
            val frequencyId = when (settings.autoBackupFrequency) {
                SettingsManager.BACKUP_FREQUENCY_DAILY -> binding.btnFrequencyDaily.id
                SettingsManager.BACKUP_FREQUENCY_MONTHLY -> binding.btnFrequencyMonthly.id
                else -> binding.btnFrequencyWeekly.id
            }
            binding.toggleFrequency.check(frequencyId)

            // Backup folder path
            settings.autoBackupFolderUri?.let { uri ->
                binding.tvBackupFolderPath.text = "Folder selected"
            }

            // Last backup timestamp
            updateLastBackupText()
        } finally {
            isInitialLoading = false
        }
    }

    private fun setupListeners() {
        // Manual backup
        binding.btnManualBackup.setOnClickListener {
            performManualBackup()
        }

        // Auto-backup toggle
        binding.switchAutoBackup.setOnCheckedChangeListener { _, isChecked ->
            if (!isInitialLoading) {
                settings.autoBackupEnabled = isChecked
                updateAutoBackupVisibility()
            }
        }

        // Frequency selection
        binding.toggleFrequency.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked && !isInitialLoading) {
                val frequency = when (checkedId) {
                    binding.btnFrequencyDaily.id -> SettingsManager.BACKUP_FREQUENCY_DAILY
                    binding.btnFrequencyMonthly.id -> SettingsManager.BACKUP_FREQUENCY_MONTHLY
                    else -> SettingsManager.BACKUP_FREQUENCY_WEEKLY
                }
                settings.autoBackupFrequency = frequency
            }
        }

        // Select backup folder
        binding.btnSelectBackupFolder.setOnClickListener {
            folderPicker.launch(null)
        }

        // Restore
        binding.btnRestore.setOnClickListener {
            filePicker.launch(arrayOf("text/csv", "text/comma-separated-values", "*/*"))
        }
    }

    private fun updateAutoBackupVisibility() {
        binding.layoutAutoBackupSettings.visibility = if (binding.switchAutoBackup.isChecked) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun updateLastBackupText() {
        val lastBackup = settings.lastBackupTimestamp
        binding.tvLastBackup.text = if (lastBackup > 0) {
            val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
            "Last backup: ${dateFormat.format(Date(lastBackup))}"
        } else {
            "Last backup: Never"
        }
    }

    private fun performManualBackup() {
        // Launch folder picker for manual backup
        folderPicker.launch(null)
    }

    private fun handleFolderSelected(folderUri: Uri) {
        // Take persistable permission
        contentResolver.takePersistableUriPermission(
            folderUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )

        // Save folder URI for auto-backup
        settings.autoBackupFolderUri = folderUri.toString()
        binding.tvBackupFolderPath.text = "Folder selected"

        // Create backup file in selected folder
        val fileName = BackupManager.generateBackupFileName()
        val fileUri = createFileInFolder(folderUri, fileName)

        if (fileUri != null) {
            executeBackup(fileUri)
        } else {
            Toast.makeText(this, "Failed to create backup file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createFileInFolder(folderUri: Uri, fileName: String): Uri? {
        return try {
            val docUri = android.provider.DocumentsContract.buildDocumentUriUsingTree(
                folderUri,
                android.provider.DocumentsContract.getTreeDocumentId(folderUri)
            )
            android.provider.DocumentsContract.createDocument(
                contentResolver,
                docUri,
                "text/csv",
                fileName
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun executeBackup(outputUri: Uri) {
        lifecycleScope.launch {
            try {
                // Get all history items
                // Get all history items
                // Get all history items
                val historyItems = historyDao.getAllHistory().first()

                // Export backup
                val result = BackupManager.exportBackup(
                    this@BackupRestoreActivity,
                    outputUri,
                    historyItems,
                    settings
                )

                result.onSuccess {
                    settings.lastBackupTimestamp = System.currentTimeMillis()
                    updateLastBackupText()
                    Toast.makeText(
                        this@BackupRestoreActivity,
                        "Backup created successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                }.onFailure { error ->
                    Toast.makeText(
                        this@BackupRestoreActivity,
                        "Backup failed: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@BackupRestoreActivity,
                    "Backup failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun handleRestoreFile(fileUri: Uri) {
        lifecycleScope.launch {
            // First validate the file
            val validationResult = BackupManager.validateBackupFile(this@BackupRestoreActivity, fileUri)

            validationResult.onSuccess { message ->
                // Show confirmation dialog
                AlertDialog.Builder(this@BackupRestoreActivity)
                    .setTitle("Restore Backup")
                    .setMessage("$message\n\nThis will replace all current settings and history. Continue?")
                    .setPositiveButton("Restore") { _, _ ->
                        performRestore(fileUri)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }.onFailure { error ->
                AlertDialog.Builder(this@BackupRestoreActivity)
                    .setTitle("Invalid Backup File")
                    .setMessage(error.message ?: "Unknown error")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    private fun performRestore(fileUri: Uri) {
        lifecycleScope.launch {
            try {
                // Import backup data
                val importResult = BackupManager.importBackup(this@BackupRestoreActivity, fileUri)

                importResult.onSuccess { backupData ->
                    // Restore settings
                    BackupManager.restoreSettings(settings, backupData.settings)

                    // Clear existing history
                    historyDao.clearHistory()

                    // Restore history items
                    var restoredCount = 0
                    for (item in backupData.historyItems) {
                        historyDao.insert(item)
                        restoredCount++
                    }

                    // Apply theme immediately
                    val currentTheme = settings.themeMode
                    android.util.Log.d("BackupRestoreActivity", "Applying theme: '$currentTheme'")
                    
                    when (currentTheme) {
                        SettingsManager.THEME_LIGHT -> androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO)
                        SettingsManager.THEME_DARK -> androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES)
                        else -> androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    }

                    // Reload UI
                    loadSettings()

                    Toast.makeText(
                        this@BackupRestoreActivity,
                        "Restored ${backupData.settings.size} settings and $restoredCount history items.",
                        Toast.LENGTH_LONG
                    ).show()

                    // Recreate this activity to reflect changes (especially theme) without killing app
                    recreate()
                }.onFailure { error ->
                    Toast.makeText(
                        this@BackupRestoreActivity,
                        "Restore failed: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@BackupRestoreActivity,
                    "Restore failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
