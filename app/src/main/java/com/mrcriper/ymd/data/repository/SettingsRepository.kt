package com.mrcriper.ymd.data.repository

import com.mrcriper.ymd.data.local.datastore.AppSettings
import com.mrcriper.ymd.data.local.datastore.SettingsDataStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class SettingsRepository @Inject constructor(
    private val store: SettingsDataStore,
) {
    val settings: Flow<AppSettings> = store.settings

    suspend fun update(block: (AppSettings) -> AppSettings) = store.update(block)
}
