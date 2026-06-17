package com.mrcriper.ymd.data.repository

import com.mrcriper.ymd.data.local.database.DownloadHistoryDao
import com.mrcriper.ymd.data.local.database.DownloadHistoryEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class DownloadRepository @Inject constructor(
    private val dao: DownloadHistoryDao,
) {
    fun observeRecent(limit: Int = 100): Flow<List<DownloadHistoryEntity>> = dao.observeRecent(limit)

    suspend fun record(entry: DownloadHistoryEntity) = dao.upsert(entry)

    suspend fun delete(trackId: String) = dao.delete(trackId)

    suspend fun isAlreadyDownloaded(trackId: String): Boolean = dao.find(trackId) != null

    suspend fun clear() = dao.clear()
}
