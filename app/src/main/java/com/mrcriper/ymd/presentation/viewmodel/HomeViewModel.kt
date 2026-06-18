package com.mrcriper.ymd.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrcriper.ymd.data.local.datastore.AppSettings
import com.mrcriper.ymd.data.remote.api.YandexEntity
import com.mrcriper.ymd.data.remote.api.YandexMusicApi
import com.mrcriper.ymd.data.repository.SettingsRepository
import com.mrcriper.ymd.domain.model.DownloadInfo
import com.mrcriper.ymd.domain.model.DownloadQuality
import com.mrcriper.ymd.domain.model.DownloadStatus
import com.mrcriper.ymd.domain.model.DownloadTask
import com.mrcriper.ymd.domain.model.FileFormat
import com.mrcriper.ymd.domain.model.LyricFormat
import com.mrcriper.ymd.domain.repository.DownloadRepository
import com.mrcriper.ymd.domain.usecase.ParseUrlUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val url: String = "",
    val detectedType: YandexEntity? = null,
    val settings: AppSettings = AppSettings(),
    val isStarting: Boolean = false,
    val message: String? = null,
    val lastApiResult: String? = null,
    val trackIds: List<String> = emptyList(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val parseUrl: ParseUrlUseCase,
    private val settingsRepository: SettingsRepository,
    private val api: YandexMusicApi,
    private val downloadRepository: DownloadRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    companion object {
        private const val TAG = "HomeViewModel"
        const val KEY_URL = "url"
    }

    private val url = MutableStateFlow(savedStateHandle.get<String>(KEY_URL).orEmpty())
    private val isStarting = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)
    private val lastApiResult = MutableStateFlow<String?>(null)
    private val trackIds = MutableStateFlow<List<String>>(emptyList())

    val state: StateFlow<HomeUiState> = combine(
        url, isStarting, message, lastApiResult, trackIds, settingsRepository.settings,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        HomeUiState(
            url = values[0] as String,
            detectedType = parseUrl(values[0] as String),
            settings = values[5] as AppSettings,
            isStarting = values[1] as Boolean,
            message = values[2] as String?,
            lastApiResult = values[3] as String?,
            trackIds = values[4] as List<String>,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun onUrlChange(value: String) {
        url.value = value
    }

    fun updateQuality(q: DownloadQuality) = viewModelScope.launch {
        settingsRepository.update { it.copy(quality = q) }
    }

    fun updateLyrics(l: LyricFormat) = viewModelScope.launch {
        settingsRepository.update { it.copy(lyricsFormat = l) }
    }

    fun update(block: (AppSettings) -> AppSettings) = viewModelScope.launch {
        settingsRepository.update(block)
    }

    fun startDownload() {
        val entity = state.value.detectedType
        if (entity == null) {
            message.value = "Invalid URL or ID"
            return
        }
        val s = state.value.settings
        viewModelScope.launch {
            isStarting.value = true
            message.value = "Resolving ${entity::class.simpleName}…"
            try {
                val resolvedTrackIds = when (entity) {
                    is YandexEntity.Track -> listOf(entity.trackId)
                    is YandexEntity.Album -> api.getAlbumsWithTracks(entity.albumId)
                        .volumes.flatten().mapNotNull { it.id }
                    is YandexEntity.Artist -> api.getArtistDirectAlbums(entity.artistId).first
                        .flatMap { album ->
                            val aid = album.id ?: return@flatMap emptyList()
                            api.getAlbumsWithTracks(aid).volumes.flatten().mapNotNull { it.id }
                        }
                    is YandexEntity.Playlist -> api.getPlaylistTracks(entity.owner, entity.kind)
                    is YandexEntity.RawId -> listOf(entity.id)
                }
                if (resolvedTrackIds.isEmpty()) {
                    message.value = "No tracks found"
                    return@launch
                }
                message.value = "Resolved ${resolvedTrackIds.size} track(s), adding to queue…"
                Log.d(TAG, "Adding ${resolvedTrackIds.size} tracks to download queue")

                // Add tasks directly to repository — DownloadViewModel will pick them up
                resolvedTrackIds.forEach { trackId ->
                    val task = DownloadTask(
                        id = trackId,
                        track = com.mrcriper.ymd.domain.model.Track(id = trackId, title = "Loading…"),
                        info = DownloadInfo(
                            trackId = trackId,
                            quality = s.quality,
                            fileFormat = FileFormat(
                                codec = s.quality.name,
                                container = com.mrcriper.ymd.domain.model.Container.MP3,
                            ),
                            urls = emptyList(),
                            decryptionKey = null,
                            bitrate = 0,
                        ),
                        targetPath = "",
                        status = DownloadStatus.QUEUED,
                    )
                    downloadRepository.addTask(task)
                    Log.d(TAG, "Added track $trackId to download queue")
                }

                trackIds.value = resolvedTrackIds
                message.value = "Added ${resolvedTrackIds.size} track(s) to download queue"

            } catch (t: Throwable) {
                Log.e(TAG, "startDownload failed", t)
                message.value = "Error: ${t::class.simpleName}: ${t.message}"
                lastApiResult.value = null
            } finally {
                isStarting.value = false
            }
        }
    }

    fun consumeMessage() {
        message.value = null
    }
}
