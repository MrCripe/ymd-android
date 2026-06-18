package com.mrcriper.ymd.data.remote.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.HttpHeaders
import io.ktor.http.headers
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YandexMusicApiTest {

    private val testToken = "y0__xDD5IXXAxje-AYg4dnqzRMOarvuZX9F2YUlwxvNlGtaOJCvzQ"

    private val testClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
                isLenient = true
                explicitNulls = false
            })
        }
        defaultRequest {
            headers {
                append(HttpHeaders.Accept, "application/json")
                append(HttpHeaders.UserAgent, "YandexMusicAndroid/24023621 (Android 14)")
                append(HttpHeaders.AcceptLanguage, "ru")
                append("X-Yandex-Music-Client", "YandexMusicAndroid/24023621")
                append(HttpHeaders.Authorization, "OAuth $testToken")
            }
        }
    }

    private val api = YandexMusicApi(
        tokenProvider = { testToken },
        config = YandexMusicConfig(),
        httpClient = testClient,
    )

    @Test
    fun testGetTrack(): Unit = runBlocking {
        val trackId = "84513093"
        val tracks = api.getTracks(listOf(trackId))
        println("Tracks response: $tracks")
        assertTrue("Should return at least one track", tracks.isNotEmpty())
        val track = tracks.first()
        assertNotNull(track.id)
        assertNotNull(track.title)
        println("Track: ${track.title} by ${track.artists.firstOrNull()?.name}")
    }

    @Test
    fun testGetDownloadInfo(): Unit = runBlocking {
        val trackId = "84513093"
        val info = api.getDownloadInfo(trackId, "lossless")
        println("Download info: codec=${info.codec}, bitrate=${info.bitrate}")
        assertNotNull(info.codec)
        val urlCount = if (info.urls.isNotEmpty()) info.urls.size else if (info.url != null) 1 else 0
        assertTrue("Should have download URLs", urlCount > 0)
        println("URLs: $urlCount")
        println("Key: ${info.key?.take(20)}...")
    }

    @Test
    fun testDownloadBytes(): Unit = runBlocking {
        val trackId = "84513093"
        val info = api.getDownloadInfo(trackId, "lossless")
        val allUrls = if (info.urls.isNotEmpty()) info.urls else listOfNotNull(info.url)
        val url = allUrls.firstOrNull()
        assertNotNull("Should have download URL", url)
        println("Downloading from: $url")
        val bytes = api.downloadBytes(url!!)
        println("Downloaded ${bytes.size} bytes")
        assertTrue("Should download meaningful amount of data", bytes.size > 10000)
    }
}
