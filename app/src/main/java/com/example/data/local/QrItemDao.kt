package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface QrItemDao {

    @Query("SELECT * FROM qr_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<QrItem>>

    @Query("SELECT * FROM qr_history WHERE isGenerated = :isGenerated ORDER BY timestamp DESC")
    fun getHistoryByType(isGenerated: Boolean): Flow<List<QrItem>>

    @Query("SELECT * FROM qr_history WHERE content LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchHistory(query: String): Flow<List<QrItem>>

    @Query("SELECT * FROM qr_history WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: Long): QrItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: QrItem): Long

    @Query("DELETE FROM qr_history WHERE id = :id")
    suspend fun deleteItem(id: Long)

    @Query("DELETE FROM qr_history")
    suspend fun clearAllHistory()

    @Query("DELETE FROM qr_history WHERE isGenerated = :isGenerated")
    suspend fun clearHistoryByType(isGenerated: Boolean)
}
