package com.mrcriper.ymd.domain.usecase

import com.mrcriper.ymd.data.remote.api.YandexMusicApi
import com.mrcriper.ymd.data.remote.download.DownloadManager
import com.mrcriper.ymd.data.remote.dto.toDownloadInfo
import com.mrcriper.ymd.domain.model.Container
import com.mrcriper.ymd.domain.model.DownloadQuality
import com.mrcriper.ymd.domain.model.DownloadTask
import com.mrcriper.ymd.domain.model.FileFormat
import com.mrcriper.ymd.domain.model.Track

class StartDownloadUseCase(
    private val api: YandexMusicApi,
    private val downloadManager: DownloadManager,
) {
    /**
     * Resolves per-track download URL, downloads+decrypts and writes the file atomically.
     * Returns the resulting [DownloadTask] including the final target path.
     */
    suspend operator fun invoke(
        track: Track,
        quality: DownloadQuality,
        targetDir: String,
        baseFilename: String,
    ): DownloadTask {
        val dto = api.getDownloadInfo(track.id, quality.apiValue)
        val info = dto.toDownloadInfo(track.id, quality)
        val payload = downloadManager.download(dto)
        val container = pickContainer(info.fileFormat, baseFilename)
        val targetFile = java.io.File(targetDir, "$baseFilename${container.extension}")
        val written = downloadManager.writeViaTempFile(payload, targetFile)
        return DownloadTask(
            id = track.id,
            track = track,
            info = info,
            targetPath = written.absolutePath,
            status = com.mrcriper.ymd.domain.model.DownloadStatus.COMPLETED,
            bytesDownloaded = payload.size.toLong(),
            totalBytes = payload.size.toLong(),
        )
    }

    /**
     * Mirrors [py-ref/ymd/core.py::to_downloadable_track] suffix selection:
     *  - MP3 → .mp3
     *  - MP4 + codec contains "flac" → .flac (pseudo-FLAC inside MP4 container)
     *  - MP4 otherwise → .m4a
     *  - FLAC → .flac
     */
    private fun pickContainer(format: FileFormat, requestedBase: String): Container {
        return when (format.container) {
            Container.MP3 -> Container.MP3
            Container.MP4 -> if (format.codec.contains("flac", ignoreCase = true)) Container.FLAC else Container.MP4
            Container.FLAC -> Container.FLAC
        }
    }
}
