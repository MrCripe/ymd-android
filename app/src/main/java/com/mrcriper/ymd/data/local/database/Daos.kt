package com.mrcriper.ymd.data.local.database

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "download_history")
data class DownloadHistoryEntity(
    @PrimaryKey val trackId: String,
    @ColumnInfo val albumId: String?,
    @ColumnInfo val artistId: String?,
    @ColumnInfo val title: String,
    @ColumnInfo val artistName: String?,
    @ColumnInfo val albumTitle: String?,
    @ColumnInfo val quality: String,
    @ColumnInfo val bitrate: Int,
    @ColumnInfo val container: String,
    @ColumnInfo val codec: String,
    @ColumnInfo val path: String,
    @ColumnInfo val sizeBytes: Long,
    @ColumnInfo val timestampMs: Long,
    @ColumnInfo val status: String,
    @ColumnInfo val errorMessage: String? = null,
)

@Entity(tableName = "track_cache")
data class TrackCacheEntity(
    @PrimaryKey val trackId: String,
    @ColumnInfo val payload: String,
    @ColumnInfo val updatedAtMs: Long,
)

@Dao
interface DownloadHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: DownloadHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entries: List<DownloadHistoryEntity>)

    @Query("SELECT * FROM download_history ORDER BY timestampMs DESC LIMIT :limit")
    fun observeRecent(limit: Int = 100): Flow<List<DownloadHistoryEntity>>

    @Query("SELECT * FROM download_history WHERE trackId = :trackId LIMIT 1")
    suspend fun find(trackId: String): DownloadHistoryEntity?

    @Query("DELETE FROM download_history WHERE trackId = :trackId")
    suspend fun delete(trackId: String)

    @Query("DELETE FROM download_history")
    suspend fun clear()
}

@Dao
interface TrackCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: TrackCacheEntity)

    @Query("SELECT * FROM track_cache WHERE trackId = :trackId LIMIT 1")
    suspend fun get(trackId: String): TrackCacheEntity?

    @Query("DELETE FROM track_cache WHERE updatedAtMs < :thresholdMs")
    suspend fun purgeOlderThan(thresholdMs: Long)
}

@Database(
    entities = [DownloadHistoryEntity::class, TrackCacheEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class YmdDatabase : RoomDatabase() {
    abstract fun downloadHistoryDao(): DownloadHistoryDao
    abstract fun trackCacheDao(): TrackCacheDao
}
