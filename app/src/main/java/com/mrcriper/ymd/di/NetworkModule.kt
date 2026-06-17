package com.mrcriper.ymd.di

import android.content.Context
import com.mrcriper.ymd.data.local.security.CryptoManager
import com.mrcriper.ymd.data.remote.api.YandexMusicApi
import com.mrcriper.ymd.data.remote.api.YandexMusicConfig
import com.mrcriper.ymd.data.remote.download.DownloadManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideCryptoManager(@ApplicationContext context: Context): CryptoManager = CryptoManager(context)

    @Provides
    @Singleton
    fun provideYandexMusicConfig(): YandexMusicConfig = YandexMusicConfig()

    @Provides
    @Singleton
    fun provideYandexMusicApi(token: TokenHolder, config: YandexMusicConfig): YandexMusicApi =
        YandexMusicApi(token.current.orEmpty(), config)

    @Provides
    @Singleton
    fun provideDownloadManager(api: YandexMusicApi): DownloadManager = DownloadManager(api)
}
