package com.mrcriper.ymd.di

import android.content.Context
import androidx.room.Room
import com.mrcriper.ymd.data.local.database.DownloadHistoryDao
import com.mrcriper.ymd.data.local.database.TrackCacheDao
import com.mrcriper.ymd.data.local.database.YmdDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): YmdDatabase =
        Room.databaseBuilder(context, YmdDatabase::class.java, "ymd.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideDownloadHistoryDao(db: YmdDatabase): DownloadHistoryDao = db.downloadHistoryDao()

    @Provides
    fun provideTrackCacheDao(db: YmdDatabase): TrackCacheDao = db.trackCacheDao()
}
