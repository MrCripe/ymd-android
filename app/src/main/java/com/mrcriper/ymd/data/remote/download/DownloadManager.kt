package com.mrcriper.ymd.data.remote.download

import com.mrcriper.ymd.data.remote.dto.DownloadInfoDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

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
class DownloadManager {

    suspend fun download(info: DownloadInfoDto): ByteArray {
        val allUrls = if (info.urls.isNotEmpty()) info.urls else listOfNotNull(info.url)
        val url = allUrls.shuffled().firstOrNull()
            ?: error("No download URL available")
        val raw = downloadBytes(url)
        val key = info.key
        return if (key.isNullOrEmpty()) raw else Decryptor.decrypt(raw, key)
    }

    private suspend fun downloadBytes(url: String): ByteArray = withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "YandexMusic/24023621 (Android 14; Pixel 8)")
            .build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
        response.body?.bytes() ?: throw IOException("Empty response body")
    }

    fun downloadWithProgress(info: DownloadInfoDto): Flow<DownloadEvent> = flow {
        val allUrls = if (info.urls.isNotEmpty()) info.urls else listOfNotNull(info.url)
        val url = allUrls.shuffled().firstOrNull()
            ?: run { emit(DownloadEvent.Failed(IllegalStateException("No URL"))); return@flow }
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build()
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "YandexMusic/24023621 (Android 14; Pixel 8)")
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val body = response.body ?: throw IOException("Empty response body")
            val contentLength = body.contentLength()
            val source = body.source()
            val buffer = ByteArray(8192)
            var totalRead = 0L
            val output = java.io.ByteArrayOutputStream()
            while (true) {
                val bytesRead = source.read(buffer)
                if (bytesRead == -1) break
                output.write(buffer, 0, bytesRead.toInt())
                totalRead += bytesRead
                emit(DownloadEvent.Progress(DownloadProgress(totalRead, contentLength, false)))
            }
            val raw = output.toByteArray()
            val key = info.key
            val payload = if (key.isNullOrEmpty()) raw else Decryptor.decrypt(raw, key)
            emit(DownloadEvent.Progress(DownloadProgress(payload.size.toLong(), payload.size.toLong(), true)))
            emit(DownloadEvent.Complete(payload))
        } catch (t: Throwable) {
            emit(DownloadEvent.Failed(t))
        }
    }.flowOn(Dispatchers.IO)

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
