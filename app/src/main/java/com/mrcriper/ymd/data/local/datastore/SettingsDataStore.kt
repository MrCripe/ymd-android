package com.mrcriper.ymd.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mrcriper.ymd.domain.model.DownloadQuality
import com.mrcriper.ymd.domain.model.LyricFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

data class AppSettings(
    val quality: DownloadQuality = DownloadQuality.BEST,
    val skipExisting: Boolean = true,
    val lyricsFormat: LyricFormat = LyricFormat.NONE,
    val embedCover: Boolean = false,
    val coverResolution: Int = 400,
    val requestDelaySeconds: Int = 0,
    val onlyMusic: Boolean = true,
    val stickToArtist: Boolean = false,
    val compatibilityLevel: Int = 1,
    val pathPattern: String = "#album-artist/#album/#number - #title",
    val unsafePath: Boolean = false,
    val saveDirUri: String? = null,
    val downloadPath: String? = null,
    val timeoutSeconds: Int = 20,
    val retries: Int = 20,
    val retryDelaySeconds: Int = 5,
    val yandexLogin: String = "MrCriper10",
    val activeAccountKey: String? = null,
)

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val QUALITY = intPreferencesKey("quality")
        val SKIP_EXISTING = booleanPreferencesKey("skip_existing")
        val LYRICS_FORMAT = stringPreferencesKey("lyrics_format")
        val EMBED_COVER = booleanPreferencesKey("embed_cover")
        val COVER_RESOLUTION = intPreferencesKey("cover_resolution")
        val DELAY = intPreferencesKey("request_delay")
        val ONLY_MUSIC = booleanPreferencesKey("only_music")
        val STICK_TO_ARTIST = booleanPreferencesKey("stick_to_artist")
        val COMPAT_LEVEL = intPreferencesKey("compat_level")
        val PATH_PATTERN = stringPreferencesKey("path_pattern")
        val UNSAFE_PATH = booleanPreferencesKey("unsafe_path")
        val SAVE_DIR = stringPreferencesKey("save_dir_uri")
        val DOWNLOAD_PATH = stringPreferencesKey("download_path")
        val TIMEOUT = intPreferencesKey("timeout")
        val RETRIES = intPreferencesKey("retries")
        val RETRY_DELAY = intPreferencesKey("retry_delay")
        val YANDEX_LOGIN = stringPreferencesKey("yandex_login")
        val ACTIVE_ACCOUNT = stringPreferencesKey("active_account")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { it.toSettings() }

    suspend fun update(block: (AppSettings) -> AppSettings) {
        context.settingsDataStore.edit { prefs ->
            val current = prefs.toSettings()
            prefs.applySettings(block(current))
        }
    }

    private fun Preferences.toSettings(): AppSettings = AppSettings(
        quality = DownloadQuality.from(this[Keys.QUALITY]),
        skipExisting = this[Keys.SKIP_EXISTING] ?: true,
        lyricsFormat = LyricFormat.from(this[Keys.LYRICS_FORMAT]),
        embedCover = this[Keys.EMBED_COVER] ?: false,
        coverResolution = this[Keys.COVER_RESOLUTION] ?: 400,
        requestDelaySeconds = this[Keys.DELAY] ?: 0,
        onlyMusic = this[Keys.ONLY_MUSIC] ?: true,
        stickToArtist = this[Keys.STICK_TO_ARTIST] ?: false,
        compatibilityLevel = this[Keys.COMPAT_LEVEL] ?: 1,
        pathPattern = this[Keys.PATH_PATTERN] ?: "#album-artist/#album/#number - #title",
        unsafePath = this[Keys.UNSAFE_PATH] ?: false,
        saveDirUri = this[Keys.SAVE_DIR],
        downloadPath = this[Keys.DOWNLOAD_PATH],
        timeoutSeconds = this[Keys.TIMEOUT] ?: 20,
        retries = this[Keys.RETRIES] ?: 20,
        retryDelaySeconds = this[Keys.RETRY_DELAY] ?: 5,
        yandexLogin = this[Keys.YANDEX_LOGIN] ?: "MrCriper10",
        activeAccountKey = this[Keys.ACTIVE_ACCOUNT],
    )

    private fun androidx.datastore.preferences.core.MutablePreferences.applySettings(s: AppSettings) {
        this[Keys.QUALITY] = s.quality.value
        this[Keys.SKIP_EXISTING] = s.skipExisting
        this[Keys.LYRICS_FORMAT] = s.lyricsFormat.name
        this[Keys.EMBED_COVER] = s.embedCover
        this[Keys.COVER_RESOLUTION] = s.coverResolution
        this[Keys.DELAY] = s.requestDelaySeconds
        this[Keys.ONLY_MUSIC] = s.onlyMusic
        this[Keys.STICK_TO_ARTIST] = s.stickToArtist
        this[Keys.COMPAT_LEVEL] = s.compatibilityLevel
        this[Keys.PATH_PATTERN] = s.pathPattern
        this[Keys.UNSAFE_PATH] = s.unsafePath
        if (s.saveDirUri != null) this[Keys.SAVE_DIR] = s.saveDirUri else remove(Keys.SAVE_DIR)
        if (s.downloadPath != null) this[Keys.DOWNLOAD_PATH] = s.downloadPath else remove(Keys.DOWNLOAD_PATH)
        this[Keys.TIMEOUT] = s.timeoutSeconds
        this[Keys.RETRIES] = s.retries
        this[Keys.RETRY_DELAY] = s.retryDelaySeconds
        this[Keys.YANDEX_LOGIN] = s.yandexLogin
        if (s.activeAccountKey != null) this[Keys.ACTIVE_ACCOUNT] = s.activeAccountKey else remove(Keys.ACTIVE_ACCOUNT)
    }
}
