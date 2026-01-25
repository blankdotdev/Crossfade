package com.blankdev.crossfade.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "history_table",
    indices = [Index(value = ["originalUrl"])]
)
data class HistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val originalUrl: String,
    val songTitle: String?,
    val artistName: String?,
    val isAlbum: Boolean = false,
    val thumbnailUrl: String?,
    val originalImageUrl: String? = null, // Original image URL for backup/restore
    val pageUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val linksJson: String?, // Store all platform links as JSON for the Share sheet
    val isResolved: Boolean = true
)

