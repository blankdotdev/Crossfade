package com.blankdev.crossfade.utils

import android.content.Context
import android.net.Uri
import com.blankdev.crossfade.CrossfadeApp
import com.blankdev.crossfade.data.HistoryItem
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Manages backup and restore operations for Crossfade settings and history data.
 * Exports data to CSV format: Crossfade.YYYY-MM-DD.csv
 */
object BackupManager {

    private const val CSV_VERSION = "1"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /**
     * Generate backup filename with current date
     */
    fun generateBackupFileName(): String {
        val date = dateFormat.format(Date())
        return "Crossfade.$date.csv"
    }

    /**
     * Export all settings and history to CSV file
     */
    suspend fun exportBackup(
        context: Context,
        outputUri: Uri,
        historyItems: List<HistoryItem>,
        settings: SettingsManager
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    // Write header
                    writer.write("# Crossfade Backup v$CSV_VERSION\n")
                    writer.write("# Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}\n")
                    writer.write("type,data\n")

                    // Export settings
                    val settingsMap = settings.getAllSettings()
                    for ((key, value) in settingsMap) {
                        val escapedValue = escapeCsvField(value.toString())
                        writer.write("SETTING,\"$key\",\"$escapedValue\"\n")
                    }

                    // Export history
                    for (item in historyItems) {
                        val row = buildHistoryRow(item)
                        writer.write("HISTORY,$row\n")
                    }

                    writer.flush()
                }
            } ?: return@withContext Result.failure(Exception("Failed to open output stream"))

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Validate backup file format
     */
    suspend fun validateBackupFile(context: Context, inputUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(inputUri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line = reader.readLine()
                    
                    // Check version header
                    if (line == null || !line.startsWith("# Crossfade Backup")) {
                        return@withContext Result.failure(Exception("Invalid backup file: Missing header"))
                    }

                    // Skip comment lines
                    while (line != null && line.startsWith("#")) {
                        line = reader.readLine()
                    }

                    // Check column header
                    if (line == null || !line.startsWith("type,data")) {
                        return@withContext Result.failure(Exception("Invalid backup file: Missing column headers"))
                    }

                    // Count rows for validation
                    var settingsCount = 0
                    var historyCount = 0
                    
                    while (reader.readLine()?.also { line = it } != null) {
                        when {
                            line.startsWith("SETTING,") -> settingsCount++
                            line.startsWith("HISTORY,") -> historyCount++
                        }
                    }

                    Result.success("Found $settingsCount settings and $historyCount history items")
                }
            } ?: Result.failure(Exception("Failed to open input stream"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Import backup from CSV file
     */
    suspend fun importBackup(
        context: Context,
        inputUri: Uri
    ): Result<BackupData> = withContext(Dispatchers.IO) {
        try {
            val settings = mutableMapOf<String, String>()
            val historyItems = mutableListOf<HistoryItem>()

            context.contentResolver.openInputStream(inputUri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line = reader.readLine()

                    // Skip header and comment lines
                    while (line != null && (line.startsWith("#") || line.startsWith("type,data"))) {
                        line = reader.readLine()
                    }

                    // Parse data rows
                    while (line != null) {
                        val parts = parseCsvLine(line)
                        
                        when (parts.getOrNull(0)) {
                            "SETTING" -> {
                                if (parts.size >= 3) {
                                    settings[parts[1]] = parts[2]
                                }
                            }
                            "HISTORY" -> {
                                val item = parseHistoryRow(parts)
                                if (item != null) {
                                    historyItems.add(item)
                                } else {
                                    android.util.Log.w("BackupManager", "Skipping invalid HISTORY row: $line")
                                }
                            }
                        }
                        
                        line = reader.readLine()
                    }
                }
            } ?: return@withContext Result.failure(Exception("Failed to open input stream"))

            Result.success(BackupData(settings, historyItems))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Restore settings from backup data
     */
    fun restoreSettings(settings: SettingsManager, backupSettings: Map<String, String>) {
        android.util.Log.d("BackupManager", "Restoring settings: $backupSettings")
        
        backupSettings["targetApp"]?.trim()?.let { settings.targetApp = it }
        backupSettings["targetPodcastApp"]?.trim()?.let { settings.targetPodcastApp = it }
        backupSettings["themeMode"]?.trim()?.let { 
            android.util.Log.d("BackupManager", "Restoring themeMode: '$it'")
            settings.themeMode = it 
        }
        backupSettings["additionalServices"]?.let { 
            // Parse comma-separated string back to Set
            val services = it.split(",").map { s -> s.trim() }.filter { s -> s.isNotBlank() }.toSet()
            settings.additionalServices = services
        }
        backupSettings["autoBackupEnabled"]?.trim()?.let { settings.autoBackupEnabled = it.toBoolean() }
        backupSettings["autoBackupFrequency"]?.trim()?.let { settings.autoBackupFrequency = it }
        backupSettings["autoBackupFolderUri"]?.trim()?.let { settings.autoBackupFolderUri = it }
        backupSettings["hasCompletedOnboarding"]?.trim()?.let { settings.hasCompletedOnboarding = it.toBoolean() }
    }

    /**
     * Build CSV row for history item
     */
    private fun buildHistoryRow(item: HistoryItem): String {
        val fields = listOf(
            item.id.toString(),
            escapeCsvField(item.originalUrl),
            escapeCsvField(item.songTitle ?: ""),
            escapeCsvField(item.artistName ?: ""),
            item.isAlbum.toString(),
            escapeCsvField(item.thumbnailUrl ?: ""),
            escapeCsvField(item.originalImageUrl ?: ""),
            escapeCsvField(item.pageUrl ?: ""),
            item.timestamp.toString(),
            escapeCsvField(item.linksJson ?: ""),
            item.isResolved.toString()
        )
        return fields.joinToString(",") { "\"$it\"" }
    }

    /**
     * Parse CSV row into HistoryItem
     */
    private fun parseHistoryRow(parts: List<String>): HistoryItem? {
        return try {
            // Skip first element which is "HISTORY"
            if (parts.size < 12) return null
            
            HistoryItem(
                id = 0, // Will be auto-generated on insert
                originalUrl = parts[2],
                songTitle = parts[3].ifBlank { null },
                artistName = parts[4].ifBlank { null },
                isAlbum = parts[5].toBoolean(),
                thumbnailUrl = parts[6].ifBlank { null },
                originalImageUrl = parts[7].ifBlank { null },
                pageUrl = parts[8].ifBlank { null },
                timestamp = parts[9].toLongOrNull() ?: System.currentTimeMillis(),
                linksJson = parts[10].ifBlank { null },
                isResolved = parts[11].toBoolean()
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Escape CSV field (handle quotes and commas)
     */
    private fun escapeCsvField(field: String): String {
        return field.replace("\"", "\"\"").replace("\n", " ").replace("\r", " ")
    }

    /**
     * Parse CSV line handling quoted fields
     */
    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val char = line[i]
            
            when {
                char == '"' && (i + 1 < line.length && line[i + 1] == '"') -> {
                    // Escaped quote
                    current.append('"')
                    i++ // Skip next quote
                }
                char == '"' -> {
                    inQuotes = !inQuotes
                }
                char == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current = StringBuilder()
                }
                else -> {
                    current.append(char)
                }
            }
            i++
        }
        
        result.add(current.toString())
        return result
    }
}

/**
 * Data class to hold backup data
 */
data class BackupData(
    val settings: Map<String, String>,
    val historyItems: List<HistoryItem>
)
