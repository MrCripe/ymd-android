package com.mrcriper.ymd.data.remote.download

import com.mrcriper.ymd.data.remote.api.YandexMusicApi
import com.mrcriper.ymd.data.remote.dto.DownloadInfoDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.security.MessageDigest

data class DownloadProgress(
    val bytesRead: Long,
    val totalBytes: Long,
    val done: Boolean = false,
)

sealed class DownloadEvent {
    data class Progress(val progress: DownloadProgress) : DownloadEvent()
    data class Complete(val bytes: ByteArray) : DownloadEvent() {
        override fun equals(other: Any?): Boolean = other is Complete && bytes.contentEquals(other.bytes)
        override fun hashCode(): Int = bytes.contentHashCode()
    }
    data class Failed(val error: Throwable) : DownloadEvent()
}

/**
 * Handles: URL selection → atomic fetch → AES-CTR decrypt (when key present).
 * Atomic write pattern mirrors [py-ref/ymd/core.py::write_via_temporary_file].
 */
class DownloadManager(private val api: YandexMusicApi) {

    suspend fun download(info: DownloadInfoDto): ByteArray {
        val url = info.downloadInfo?.urls?.shuffled()?.firstOrNull()
            ?: error("No download URL available")
        val raw = api.downloadBytes(url)
        val key = info.downloadInfo?.key
        return if (key.isNullOrEmpty()) raw else Decryptor.decrypt(raw, key)
    }

    fun downloadWithProgress(info: DownloadInfoDto): Flow<DownloadEvent> = flow {
        val url = info.downloadInfo?.urls?.shuffled()?.firstOrNull()
            ?: run { emit(DownloadEvent.Failed(IllegalStateException("No URL"))); return@flow }
        try {
            val raw = api.downloadBytes(url)
            val key = info.downloadInfo?.key
            val payload = if (key.isNullOrEmpty()) raw else Decryptor.decrypt(raw, key)
            emit(DownloadEvent.Progress(DownloadProgress(payload.size.toLong(), payload.size.toLong(), true)))
            emit(DownloadEvent.Complete(payload))
        } catch (t: Throwable) {
            emit(DownloadEvent.Failed(t))
        }
    }

    /**
     * Writes [data] to a temp file with sha256(name) suffix and renames atomically.
     * [hook] may rewrite/relocate the temp file before rename (used for FLAC remux).
     */
    fun writeViaTempFile(
        data: ByteArray,
        target: File,
        hook: ((File) -> File)? = null,
    ): File {
        require(target.parentFile?.exists() == true || target.parentFile?.mkdirs() == true) {
            "Parent dir not writable: ${target.parent}"
        }
        val tempName = ".yandex-music-downloader.${sha256Hex(target.name)}.tmp"
        val temp = File(target.parentFile, tempName)
        try {
            temp.writeBytes(data)
            val finalFile = hook?.invoke(temp) ?: target
            if (temp.exists()) temp.renameTo(finalFile)
            return finalFile
        } catch (e: InterruptedException) {
            temp.delete()
            throw e
        }
    }

    private fun sha256Hex(s: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(s.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
