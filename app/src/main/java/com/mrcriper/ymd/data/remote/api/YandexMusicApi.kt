package com.mrcriper.ymd.data.remote.api

import com.mrcriper.ymd.data.remote.dto.AlbumDto
import com.mrcriper.ymd.data.remote.dto.AlbumWithTracksDto
import com.mrcriper.ymd.data.remote.dto.AlbumsResponseDto
import com.mrcriper.ymd.data.remote.dto.ArtistDto
import com.mrcriper.ymd.data.remote.dto.ArtistsResponseDto
import com.mrcriper.ymd.data.remote.dto.DownloadInfoDto
import com.mrcriper.ymd.data.remote.dto.DownloadInfoResponseDto
import com.mrcriper.ymd.data.remote.dto.LyricsResponseDto
import com.mrcriper.ymd.data.remote.dto.PagerDto
import com.mrcriper.ymd.data.remote.dto.PlaylistDto
import com.mrcriper.ymd.data.remote.dto.PlaylistResponseDto
import com.mrcriper.ymd.data.remote.dto.TrackDto
import com.mrcriper.ymd.data.remote.dto.TracksResponseDto
import com.mrcriper.ymd.data.remote.dto.toAlbumWithTracks
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.HttpHeaders
import io.ktor.http.headers
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class YandexMusicConfig(
    val timeoutMillis: Long = 20_000L,
    val retries: Int = 20,
    val retryDelayMillis: Long = 5_000L,
    val userAgent: String = "YandexMusic/24023621 (Android 14; Pixel 8)",
    val xClientId: String = "YandexMusicAndroid/24023621",
)

class YandexMusicApi(
    private val token: String,
    private val config: YandexMusicConfig = YandexMusicConfig(),
    val httpClient: HttpClient = defaultHttpClient(config, token),
) {

    suspend fun getTracks(trackIds: Collection<String>): List<TrackDto> {
        if (trackIds.isEmpty()) return emptyList()
        val response: TracksResponseDto = httpClient.get("$BASE_HOST/tracks") {
            parameter("track-ids", trackIds.joinToString(","))
        }.body()
        try {
            val logFile = java.io.File("/data/data/com.mrcriper.ymd.debug/files/api_log.txt")
            java.io.FileWriter(logFile, true).use {
                it.write("${System.currentTimeMillis()} getTracks: ${response.result.size} tracks, full=${response}\n")
            }
        } catch (_: Exception) {}
        return response.result
    }

    suspend fun getAlbumsWithTracks(albumId: String): AlbumWithTracksDto {
        val dto: AlbumDto = httpClient.get("$BASE_HOST/albums/$albumId/with-tracks").body()
        return dto.toAlbumWithTracks()
    }

    suspend fun getArtist(artistId: String): ArtistDto? {
        val response: ArtistsResponseDto = httpClient.get("$BASE_HOST/artists/$artistId").body()
        return response.result.firstOrNull()
    }

    suspend fun getArtistDirectAlbums(
        artistId: String,
        page: Int = 0,
        pageSize: Int = FETCH_PAGE_SIZE,
    ): Pair<List<AlbumDto>, PagerDto?> {
        val response: AlbumsResponseDto = httpClient.get("$BASE_HOST/artists/$artistId/direct-albums") {
            parameter("page", page)
            parameter("page-size", pageSize)
        }.body()
        return response.result to response.pager
    }

    suspend fun getUserPlaylist(owner: String, kind: String): PlaylistDto? {
        val response: PlaylistResponseDto = httpClient.get("$BASE_HOST/users/$owner/playlists/$kind").body()
        return response.result
    }

    suspend fun getPlaylistTracks(
        owner: String,
        kind: String,
        page: Int = 0,
        pageSize: Int = FETCH_PAGE_SIZE,
    ): List<String> {
        val response: PlaylistResponseDto = httpClient.get("$BASE_HOST/users/$owner/playlists/$kind") {
            parameter("page", page)
            parameter("page-size", pageSize)
        }.body()
        return response.result?.tracks
            ?.mapNotNull { it.id }
            ?: emptyList()
    }

    suspend fun getDownloadInfo(trackId: String, quality: String): DownloadInfoDto {
        val ts = System.currentTimeMillis() / 1000L
        val params = mapOf(
            "ts" to ts,
            "trackId" to trackId,
            "quality" to quality,
            "codecs" to CODECS,
            "transports" to TRANSPORTS,
        )
        val sign = Signing.sign(params)
        // Only encode + as %2B, don't touch / and other chars (URLEncoder would encode / as %2F which breaks the signature)
        val encodedSign = sign.replace("+", "%2B")
        val url = "$BASE_HOST/get-file-info?ts=$ts&trackId=$trackId&quality=$quality&codecs=$CODECS&transports=$TRANSPORTS&sign=$encodedSign"
        // Log to file for debugging
        try {
            val logFile = java.io.File("/data/data/com.mrcriper.ymd.debug/files/api_log.txt")
            logFile.parentFile?.mkdirs()
            java.io.FileWriter(logFile, true).use {
                it.write("${System.currentTimeMillis()} URL: $url\n")
                it.write("${System.currentTimeMillis()} sign: $sign\n")
                it.write("${System.currentTimeMillis()} token: ${token.take(8)}\n")
            }
        } catch (_: Exception) {}
        // Use OkHttp directly to match Python behavior exactly
        val body = withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .build()
                val request = okhttp3.Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "OAuth $token")
                    .addHeader("Accept", "application/json")
                    .addHeader("Accept-Language", "ru")
                    .addHeader("X-Yandex-Music-Client", config.xClientId)
                    .addHeader("User-Agent", config.userAgent)
                    .build()
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""
                try {
                    val logFile = java.io.File("/data/data/com.mrcriper.ymd.debug/files/api_log.txt")
                    java.io.FileWriter(logFile, true).use {
                        it.write("${System.currentTimeMillis()} HTTP ${response.code}: ${responseBody.take(300)}\n")
                    }
                } catch (_: Exception) {}
                if (!response.isSuccessful) {
                    throw IllegalStateException("HTTP ${response.code}: $responseBody")
                }
                responseBody
            } catch (e: Exception) {
                try {
                    val logFile = java.io.File("/data/data/com.mrcriper.ymd.debug/files/api_log.txt")
                    java.io.FileWriter(logFile, true).use {
                        it.write("${System.currentTimeMillis()} EXCEPTION: ${e::class.simpleName}: ${e.message}\n")
                    }
                } catch (_: Exception) {}
                throw e
            }
        }
        val response: DownloadInfoResponseDto = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }.decodeFromString(body)
        val apiResult = response.result
        if (apiResult?.name != null && apiResult.message != null) {
            throw IllegalStateException("API error: ${apiResult.name} - ${apiResult.message}")
        }
        val info = apiResult?.downloadInfo
            ?: throw IllegalStateException("No download info for track $trackId")
        if (info.urls.isEmpty() && info.url == null) {
            throw IllegalStateException("No download URLs for track $trackId")
        }
        return info
    }

    suspend fun downloadBytes(url: String): ByteArray {
        val response: HttpResponse = httpClient.get(url)
        check(response.status.isSuccess()) { "CDN HTTP ${response.status}" }
        return response.bodyAsBytes()
    }

    suspend fun getTrackLyrics(trackId: String, format: String): String? {
        val response: LyricsResponseDto = httpClient.get("$BASE_HOST/tracks/$trackId/lyrics") {
            parameter("format", format)
        }.body()
        return response.result?.fullLyrics ?: response.result?.lyrics
    }

    companion object {
        const val BASE_HOST: String = "https://api.music.yandex.net"
        const val FETCH_PAGE_SIZE: Int = 10
        const val CODECS: String = "flac,flac-mp4,mp3,aac,he-aac,aac-mp4,he-aac-mp4"
        const val TRANSPORTS: String = "encraw"

        fun defaultHttpClient(config: YandexMusicConfig, token: String): HttpClient = HttpClient(Android) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                    isLenient = true
                    explicitNulls = false
                })
            }
            install(HttpTimeout) {
                requestTimeoutMillis = config.timeoutMillis
                connectTimeoutMillis = config.timeoutMillis
                socketTimeoutMillis = config.timeoutMillis
            }
            install(HttpRequestRetry) {
                retryOnExceptionOrServerErrors(maxRetries = config.retries)
                exponentialDelay(base = 2.0, maxDelayMs = config.retryDelayMillis * 2)
            }
            install(Logging) {
                level = LogLevel.NONE
            }
            defaultRequest {
                headers {
                    append(HttpHeaders.Accept, "application/json")
                    append(HttpHeaders.UserAgent, config.userAgent)
                    append(HttpHeaders.AcceptLanguage, "ru")
                    append("X-Yandex-Music-Client", config.xClientId)
                    if (token.isNotBlank()) {
                        append(HttpHeaders.Authorization, "OAuth $token")
                    }
                }
            }
            engine {
                connectTimeout = config.timeoutMillis.toInt()
                socketTimeout = config.timeoutMillis.toInt()
            }
        }
    }
}
