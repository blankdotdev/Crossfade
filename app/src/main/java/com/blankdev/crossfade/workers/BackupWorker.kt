package com.blankdev.crossfade.workers

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.blankdev.crossfade.CrossfadeApp
import com.blankdev.crossfade.utils.BackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Worker for performing automatic backups
 */
class BackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val settings = CrossfadeApp.instance.settingsManager
            val historyDao = CrossfadeApp.instance.database.historyDao()

            // Check if auto-backup is enabled
            if (!settings.autoBackupEnabled) {
                return@withContext Result.success()
            }

            // Get backup folder URI
            val folderUriString = settings.autoBackupFolderUri
            if (folderUriString.isNullOrBlank()) {
                return@withContext Result.failure()
            }

            val folderUri = Uri.parse(folderUriString)

            // Create backup file
            val fileName = BackupManager.generateBackupFileName()
            val fileUri = createFileInFolder(folderUri, fileName)
                ?: return@withContext Result.failure()

            // Get all history items
            val historyItems = historyDao.getAllHistory().first()

            // Export backup
            val result = BackupManager.exportBackup(
                applicationContext,
                fileUri,
                historyItems,
                settings
            )

            if (result.isSuccess) {
                settings.lastBackupTimestamp = System.currentTimeMillis()
                Result.success()
            } else {
                Result.failure()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    private fun createFileInFolder(folderUri: Uri, fileName: String): Uri? {
        return try {
            val docUri = android.provider.DocumentsContract.buildDocumentUriUsingTree(
                folderUri,
                android.provider.DocumentsContract.getTreeDocumentId(folderUri)
            )
            android.provider.DocumentsContract.createDocument(
                applicationContext.contentResolver,
                docUri,
                "text/csv",
                fileName
            )
        } catch (e: Exception) {
            null
        }
    }
}
