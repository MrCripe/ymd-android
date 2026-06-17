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

class YandexMusicConfig(
    val timeoutMillis: Long = 20_000L,
    val retries: Int = 20,
    val retryDelayMillis: Long = 5_000L,
    val userAgent: String = "YMD-Android/1.0",
    val xClientId: String = "YMD/1.0",
)

class YandexMusicApi(
    private val token: String,
    private val config: YandexMusicConfig = YandexMusicConfig(),
    val httpClient: HttpClient = defaultHttpClient(config),
) {

    suspend fun getTracks(trackIds: Collection<String>): List<TrackDto> {
        if (trackIds.isEmpty()) return emptyList()
        val response: TracksResponseDto = httpClient.get("$BASE_HOST/tracks") {
            parameter("track-ids", trackIds.joinToString(","))
        }.body()
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
        val response: DownloadInfoResponseDto = httpClient.get("$BASE_HOST/get-file-info") {
            parameter("ts", ts)
            parameter("trackId", trackId)
            parameter("quality", quality)
            parameter("codecs", CODECS)
            parameter("transports", TRANSPORTS)
            parameter("sign", sign)
        }.body()
        val list = response.result
        require(list.isNotEmpty()) { "Empty download info for track $trackId" }
        val match = list.firstOrNull { it.error == null }
            ?: throw IllegalStateException("No usable codec for track $trackId: ${list.first().error}")
        return match
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

        fun defaultHttpClient(config: YandexMusicConfig): HttpClient = HttpClient(Android) {
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
                }
            }
            engine {
                connectTimeout = config.timeoutMillis.toInt()
                socketTimeout = config.timeoutMillis.toInt()
            }
        }
    }
}
