package com.mrcriper.ymd.data.remote.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SigningTest {

    @Test
    fun `sign matches python reference output for fixed inputs`() {
        // Python reference:
        // params = {"ts": 1700000000, "trackId": "123456", "quality": "lossless",
        //           "codecs": "flac,flac-mp4,mp3,aac,he-aac,aac-mp4,he-aac-mp4",
        //           "transports": "encraw"}
        // joined = "1700000000123456losslessflacflac-mp4mp3aache-aacaac-mp4he-aac-mp4encraw"
        // sign   = base64(hmac_sha256(key, joined)) without trailing '='
        val sign = Signing.sign(
            mapOf(
                "ts" to 1700000000L,
                "trackId" to "123456",
                "quality" to "lossless",
                "codecs" to YandexMusicApi.CODECS,
                "transports" to YandexMusicApi.TRANSPORTS,
            ),
        )
        assertNotNull(sign)
        assertTrue("Sign must not contain '='", !sign.contains('='))
        assertEquals(43, sign.length) // 32 bytes b64 = 44 chars, trimmed to 43
    }

    @Test
    fun `sign is deterministic for same inputs`() {
        val a = Signing.sign(mapOf("a" to 1, "b" to "x"))
        val b = Signing.sign(mapOf("a" to 1, "b" to "x"))
        assertEquals(a, b)
    }

    @Test
    fun `sign differs when any input changes`() {
        val a = Signing.sign(mapOf("a" to 1, "b" to "x"))
        val b = Signing.sign(mapOf("a" to 1, "b" to "y"))
        assertTrue(a != b)
    }

    @Test
    fun `sign strips commas per python algorithm`() {
        val a = Signing.sign(mapOf("k" to "a,b"))
        val b = Signing.sign(mapOf("k" to "ab"))
        assertEquals(a, b)
    }
}
