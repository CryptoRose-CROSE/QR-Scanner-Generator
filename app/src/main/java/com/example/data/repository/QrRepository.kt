package com.example.data.repository

import com.example.data.local.QrItem
import com.example.data.local.QrItemDao
import kotlinx.coroutines.flow.Flow

class QrRepository(private val qrItemDao: QrItemDao) {

    fun getAllHistory(): Flow<List<QrItem>> = qrItemDao.getAllHistory()

    fun getHistoryByType(isGenerated: Boolean): Flow<List<QrItem>> =
        qrItemDao.getHistoryByType(isGenerated)

    fun searchHistory(query: String): Flow<List<QrItem>> =
        qrItemDao.searchHistory(query)

    suspend fun getItemById(id: Long): QrItem? = qrItemDao.getItemById(id)

    suspend fun insertItem(item: QrItem): Long = qrItemDao.insertItem(item)

    suspend fun deleteItem(id: Long) = qrItemDao.deleteItem(id)

    suspend fun clearAllHistory() = qrItemDao.clearAllHistory()

    suspend fun clearHistoryByType(isGenerated: Boolean) =
        qrItemDao.clearHistoryByType(isGenerated)
}
