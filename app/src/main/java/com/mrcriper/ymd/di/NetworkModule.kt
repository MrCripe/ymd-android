package com.mrcriper.ymd.di

import android.content.Context
import com.mrcriper.ymd.data.local.datastore.SettingsDataStore
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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
    fun provideYandexMusicApi(
        token: TokenHolder,
        config: YandexMusicConfig,
        crypto: CryptoManager,
        settings: SettingsDataStore,
    ): YandexMusicApi {
        // Hydrate TokenHolder with the previously active account's token on first read.
        val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        appScope.launch {
            val activeKey = settings.settings.first().activeAccountKey
            if (!activeKey.isNullOrBlank()) {
                token.current = crypto.getToken(activeKey)
            }
        }
        return YandexMusicApi(token = token.current ?: "y0__xDD5IXXAxje-AYg4dnqzRMOarvuZX9F2YUlwxvNlGtaOJCvzQ", config = config)
    }

    @Provides
    @Singleton
    fun provideDownloadManager(): DownloadManager = DownloadManager()
}