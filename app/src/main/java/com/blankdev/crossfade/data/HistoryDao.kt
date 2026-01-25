package com.blankdev.crossfade.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history_table ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: HistoryItem)

    @androidx.room.Update
    suspend fun update(item: HistoryItem)

    @Query("SELECT * FROM history_table WHERE originalUrl = :url LIMIT 1")
    suspend fun getHistoryItemByUrl(url: String): HistoryItem?

    @androidx.room.Delete
    suspend fun delete(item: HistoryItem)

    @Query("DELETE FROM history_table")
    suspend fun clearHistory()
}
