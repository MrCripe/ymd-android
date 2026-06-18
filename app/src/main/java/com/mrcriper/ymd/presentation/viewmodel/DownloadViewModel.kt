package com.mrcriper.ymd.presentation.viewmodel

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrcriper.ymd.data.local.database.DownloadHistoryEntity
import com.mrcriper.ymd.data.remote.api.YandexMusicApi
import com.mrcriper.ymd.domain.util.CoverArt
import okhttp3.OkHttpClient
import okhttp3.Request
import com.mrcriper.ymd.data.remote.download.DownloadEvent
import com.mrcriper.ymd.data.remote.download.DownloadManager
import com.mrcriper.ymd.data.remote.dto.toDownloadInfo
import com.mrcriper.ymd.data.remote.dto.toTrack
import com.mrcriper.ymd.data.repository.DownloadRepository as DataDownloadRepository
import com.mrcriper.ymd.domain.model.DownloadStatus
import com.mrcriper.ymd.domain.model.DownloadTask
import com.mrcriper.ymd.domain.repository.DownloadRepository
import com.mrcriper.ymd.domain.util.PathPatternParser
import com.mrcriper.ymd.domain.util.TagWriter
import com.mrcriper.ymd.service.DownloadForegroundService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import javax.inject.Inject

data class DownloadItem(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val bitrate: Int,
    val format: String,
    val progress: Float,
    val status: Status,
    val errorMessage: String? = null,
    val speedBytesPerSec: Long = 0,
    val coverUrl: String? = null,
) {
    enum class Status { QUEUED, RUNNING, PAUSED, COMPLETED, FAILED, CANCELLED }
}

data class DownloadUiState(val items: List<DownloadItem> = emptyList())

@HiltViewModel
class DownloadViewModel @Inject constructor(
    private val api: YandexMusicApi,
    private val downloadManager: DownloadManager,
    private val repository: DownloadRepository,
    private val dataRepository: DataDownloadRepository,
    private val tokenHolder: com.mrcriper.ymd.di.TokenHolder,
    @dagger.hilt.android.qualifiers.ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val currentToken: String get() = tokenHolder.current ?: ""

    companion object {
        private const val TAG = "DownloadViewModel"
    }

    private val _events = MutableSharedFlow<Event>()
    val events = _events.asSharedFlow()

    private val pausedIds = mutableSetOf<String>()
    private val cancelledIds = mutableSetOf<String>()
    private val downloadJobs = mutableMapOf<String, Job>()
    private val downloadStartTime = mutableMapOf<String, Long>()

    sealed class Event {
        data class DownloadComplete(val path: String) : Event()
        data class DownloadError(val message: String) : Event()
    }

    val state: StateFlow<DownloadUiState> = repository.tasks
        .map { tasksMap ->
            DownloadUiState(items = tasksMap.values.map { it.toDownloadItem() })
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DownloadUiState())

    private fun log(msg: String) {
        Log.d(TAG, msg)
        try {
            val logFile = File(appContext.filesDir, "download_log.txt")
            PrintWriter(FileWriter(logFile, true)).use {
                it.println("${System.currentTimeMillis()} $TAG: $msg")
            }
        } catch (_: Exception) {}
    }

    private fun DownloadTask.toDownloadItem(): DownloadItem = DownloadItem(
        id = id,
        title = track.title,
        artist = track.primaryArtist?.name ?: "",
        album = track.primaryAlbum?.title ?: "",
        bitrate = info.bitrate,
        format = info.fileFormat.codec,
        progress = progress,
        status = when (status) {
            DownloadStatus.QUEUED -> DownloadItem.Status.QUEUED
            DownloadStatus.RUNNING -> DownloadItem.Status.RUNNING
            DownloadStatus.PAUSED -> DownloadItem.Status.PAUSED
            DownloadStatus.COMPLETED -> DownloadItem.Status.COMPLETED
            DownloadStatus.FAILED -> DownloadItem.Status.FAILED
            DownloadStatus.CANCELLED -> DownloadItem.Status.CANCELLED
        },
        errorMessage = errorMessage,
        speedBytesPerSec = speedBytesPerSec,
        coverUrl = track.coverUri,
    )

    init {
        log("init: starting to observe repository.tasks")
        viewModelScope.launch {
            repository.tasks.collect { tasks ->
                log("tasks updated: ${tasks.size} tasks, statuses=${tasks.values.map { it.status }}")
                tasks.values.filter { it.status == DownloadStatus.QUEUED }.forEach { task ->
                    if (!downloadJobs.containsKey(task.id)) {
                        log("auto-starting download for track ${task.id}, quality=${task.info.quality}")
                        startForegroundService()
                        // Register job immediately to prevent double-start
                        val job = Job()
                        downloadJobs[task.id] = job
                        viewModelScope.launch(job) {
                            processDownload(task)
                        }
                    }
                }
            }
        }
    }

    private fun startForegroundService() {
        try {
            val serviceIntent = Intent(appContext, DownloadForegroundService::class.java)
            appContext.startForegroundService(serviceIntent)
        } catch (e: Exception) {
            log("Failed to start foreground service: ${e.message}")
        }
    }

    private fun stopForegroundService() {
        try {
            val stopIntent = Intent(appContext, DownloadForegroundService::class.java).apply {
                action = DownloadForegroundService.ACTION_STOP
            }
            appContext.stopService(stopIntent)
        } catch (e: Exception) {
            log("Failed to stop foreground service: ${e.message}")
        }
    }

    private suspend fun processDownload(task: DownloadTask) {
        val id = task.id
        log("processDownload: START track $id, quality=${task.info.quality}")
        repository.updateTask(id) { it.copy(status = DownloadStatus.RUNNING) }

        try {
            // Fetch track info
            log("processDownload: fetching track info for $id")
            val trackDto = api.getTracks(listOf(id)).firstOrNull()
                ?: throw IllegalStateException("Track not found")
            val track = trackDto.toTrack()
            log("processDownload: track title=${track.title}")

            // Get download info
            log("processDownload: getting download info for $id, quality=${task.info.quality.apiValue}")
            log("processDownload: token=${currentToken.take(8)}...")
            val infoDto = api.getDownloadInfo(id, task.info.quality.apiValue)
            log("processDownload: got downloadInfo, codec=${infoDto.codec}, urls=${infoDto.urls.size}, key=${infoDto.key?.take(8)}")
            val info = infoDto.toDownloadInfo(id, task.info.quality)
            log("processDownload: mapped info, codec=${info.fileFormat.codec}, container=${info.fileFormat.container}")

            repository.updateTask(id) { it.copy(track = track, info = info) }

            // Download data with progress
            log("processDownload: downloading data for $id")
            downloadStartTime[id] = System.currentTimeMillis()
            var result = ByteArray(0)
            val downloadFlow: kotlinx.coroutines.flow.Flow<DownloadEvent> = downloadManager.downloadWithProgress(infoDto)
            downloadFlow.collect { event: DownloadEvent ->
                when (event) {
                    is DownloadEvent.Complete -> {
                        result = event.bytes
                        log("processDownload: downloaded ${result.size} bytes for $id")
                    }
                    is DownloadEvent.Progress -> {
                        val now = System.currentTimeMillis()
                        val startTime = downloadStartTime[id] ?: now
                        val elapsedMs = now - startTime
                        val speed = if (elapsedMs > 0) {
                            event.progress.bytesRead * 1000 / elapsedMs
                        } else 0L
                        repository.updateTask(id) {
                            it.copy(
                                bytesDownloaded = event.progress.bytesRead,
                                totalBytes = event.progress.totalBytes,
                                speedBytesPerSec = speed,
                            )
                        }
                    }
                    is DownloadEvent.Failed -> throw event.error
                }
            }
            val data = result

            // Check if cancelled
            if (cancelledIds.contains(id)) {
                log("processDownload: cancelled after download for $id")
                return
            }

            // Determine save location — write to app-specific dir first, then copy to public
            val publicDir = File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MUSIC), "YMD")
            if (!publicDir.exists()) publicDir.mkdirs()
            val appDir = File(appContext.getExternalFilesDir("Music"), "YMD")
            if (!appDir.exists()) appDir.mkdirs()

            val filename = PathPatternParser.parse(
                pattern = com.mrcriper.ymd.domain.model.PathPattern(
                    template = "#album-artist/#album/#number - #title",
                    unsafe = false,
                ),
                track = track,
            )
            val extension = when (info.fileFormat.container) {
                com.mrcriper.ymd.domain.model.Container.FLAC -> ".flac"
                com.mrcriper.ymd.domain.model.Container.MP3 -> ".mp3"
                com.mrcriper.ymd.domain.model.Container.MP4 -> ".m4a"
            }
            val targetFile = File(appDir, "$filename$extension")
            log("processDownload: saving to ${targetFile.absolutePath}")

            // Write atomically to app-specific dir
            val written = downloadManager.writeViaTempFile(data, targetFile)
            log("processDownload: written ${written.length()} bytes")

            // Copy to public Music directory via MediaStore so it's visible to other apps
            try {
                val publicTargetFile = File(publicDir, "$filename$extension")
                val resolver = appContext.contentResolver
                val collection = android.provider.MediaStore.Audio.Media.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val values = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Audio.Media.DISPLAY_NAME, "$filename$extension")
                    put(android.provider.MediaStore.Audio.Media.MIME_TYPE, "audio/mp4a-latm")
                    put(android.provider.MediaStore.Audio.Media.RELATIVE_PATH, "Music/YMD")
                    put(android.provider.MediaStore.Audio.Media.IS_PENDING, 1)
                }
                val uri = resolver.insert(collection, values)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { os ->
                        written.inputStream().use { ins -> ins.copyTo(os) }
                    }
                    values.clear()
                    values.put(android.provider.MediaStore.Audio.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                    log("processDownload: copied to public dir via MediaStore")
                }
            } catch (e: Exception) {
                log("processDownload: MediaStore copy failed: ${e.message}")
            }

            // Write tags
            try {
                // Download cover art
                val cover = track.coverUri?.let { coverUri ->
                    log("processDownload: downloading cover from $coverUri")
                    runCatching {
                        // Replace %% with orig for original size, prepend https if needed
                        val fixedUri = coverUri.replace("%%", "orig")
                        val fullCoverUrl = if (fixedUri.startsWith("http")) fixedUri else "https://$fixedUri"
                        log("processDownload: cover full URL: $fullCoverUrl")
                        val client = okhttp3.OkHttpClient()
                        val request = okhttp3.Request.Builder().url(fullCoverUrl)
                            .addHeader("User-Agent", "YandexMusic/24023621 (Android 14; Pixel 8)").build()
                        val response = client.newCall(request).execute()
                        log("processDownload: cover response code: ${response.code}")
                        if (response.isSuccessful) {
                            val bytes = response.body?.bytes() ?: return@let null
                            log("processDownload: cover downloaded ${bytes.size} bytes")
                            val mime = when {
                                bytes.take(4).toByteArray().contentEquals(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)) -> "image/png"
                                bytes.take(3).toByteArray().contentEquals(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())) -> "image/jpeg"
                                else -> "image/jpeg"
                            }
                            CoverArt(bytes, mime)
                        } else {
                            log("processDownload: cover download failed: ${response.code}")
                            null
                        }
                    }.getOrNull()
                }
                log("processDownload: cover=${cover?.bytes?.size ?: "null"}, container=${info.fileFormat.container}")
                TagWriter().write(
                    file = written,
                    container = info.fileFormat.container,
                    codec = info.fileFormat.codec,
                    title = track.title,
                    album = track.primaryAlbum?.title ?: "",
                    artists = track.artists.map { it.name },
                    albumArtists = track.primaryAlbum?.artists?.map { it.name } ?: emptyList(),
                    trackNumber = track.trackNumber,
                    discNumber = null,
                    year = track.primaryAlbum?.year,
                    genre = track.primaryAlbum?.genre,
                    lyrics = null,
                    url = "https://music.yandex.ru/track/${track.id}",
                    cover = cover,
                )
            } catch (e: Exception) {
                log("processDownload: tag writing failed: ${e.message}")
            }

            // Save to database for library
            try {
                dataRepository.record(
                    DownloadHistoryEntity(
                        trackId = track.id,
                        albumId = track.primaryAlbum?.id,
                        artistId = track.artists.firstOrNull()?.id,
                        title = track.title,
                        artistName = track.primaryArtist?.name,
                        albumTitle = track.primaryAlbum?.title,
                        quality = task.info.quality.apiValue,
                        bitrate = info.bitrate,
                        container = info.fileFormat.container.extension,
                        codec = info.fileFormat.codec,
                        path = written.absolutePath,
                        sizeBytes = written.length(),
                        timestampMs = System.currentTimeMillis(),
                        status = "COMPLETED",
                    )
                )
            } catch (e: Exception) {
                log("processDownload: db record failed: ${e.message}")
            }

            // Finalize
            repository.updateTask(id) {
                it.copy(status = DownloadStatus.COMPLETED, targetPath = written.absolutePath)
            }
            log("processDownload: COMPLETED $id")
            _events.emit(Event.DownloadComplete(written.absolutePath))

        } catch (e: CancellationException) {
            log("processDownload: CANCELLED $id")
            repository.updateTask(id) {
                it.copy(status = DownloadStatus.CANCELLED, errorMessage = "Cancelled")
            }
            throw e // re-throw to propagate cancellation
        } catch (e: Exception) {
            log("processDownload: FAILED $id: ${e::class.simpleName}: ${e.message}")
            repository.updateTask(id) {
                it.copy(status = DownloadStatus.FAILED, errorMessage = e.message)
            }
            _events.emit(Event.DownloadError(e.message ?: "Unknown error"))
        } finally {
            downloadJobs.remove(id)
            val hasActive = repository.tasks.value.values.any {
                it.status == DownloadStatus.RUNNING || it.status == DownloadStatus.QUEUED
            }
            if (!hasActive) {
                stopForegroundService()
            }
        }
    }

    fun pause(id: String) {
        log("pause: $id")
        pausedIds.add(id)
        repository.updateTask(id) { it.copy(status = DownloadStatus.PAUSED) }
    }

    fun resume(id: String) {
        log("resume: $id")
        pausedIds.remove(id)
        repository.updateTask(id) { it.copy(status = DownloadStatus.RUNNING) }
    }

    fun cancel(id: String) {
        log("cancel: $id, job=${downloadJobs[id]}")
        cancelledIds.add(id)
        downloadJobs[id]?.cancel()
        repository.updateTask(id) { it.copy(status = DownloadStatus.CANCELLED) }
    }

    fun cancelAll() {
        log("cancelAll: ${repository.tasks.value.keys}")
        repository.tasks.value.keys.forEach { cancelledIds.add(it) }
        downloadJobs.values.forEach { it.cancel() }
        repository.tasks.value.keys.forEach {
            repository.updateTask(it) { it.copy(status = DownloadStatus.CANCELLED) }
        }
    }
}
