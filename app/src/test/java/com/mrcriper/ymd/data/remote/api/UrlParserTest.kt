package com.mrcriper.ymd.data.remote.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlParserTest {

    @Test fun `parses track URL`() {
        val r = detectYandexEntityType("https://music.yandex.ru/album/12345/track/67890")
        assertTrue(r is YandexEntity.Track)
        assertEquals("67890", (r as YandexEntity.Track).trackId)
    }

    @Test fun `parses album URL`() {
        val r = detectYandexEntityType("https://music.yandex.ru/album/12345")
        assertTrue(r is YandexEntity.Album)
        assertEquals("12345", (r as YandexEntity.Album).albumId)
    }

    @Test fun `parses artist URL`() {
        val r = detectYandexEntityType("https://music.yandex.ru/artist/42")
        assertTrue(r is YandexEntity.Artist)
        assertEquals("42", (r as YandexEntity.Artist).artistId)
    }

    @Test fun `parses playlist URL`() {
        val r = detectYandexEntityType("https://music.yandex.ru/users/MrCriper10/playlists/3")
        assertTrue(r is YandexEntity.Playlist)
        val p = r as YandexEntity.Playlist
        assertEquals("MrCriper10", p.owner)
        assertEquals("3", p.kind)
    }

    @Test fun `bare numeric ID is treated as track`() {
        val r = detectYandexEntityType("12345")
        assertTrue(r is YandexEntity.RawId)
        assertEquals("12345", (r as YandexEntity.RawId).id)
        assertEquals(YandexEntity.Kind.Track, r.kind)
    }

    @Test fun `unrecognized input returns null`() {
        assertNull(detectYandexEntityType("https://example.com"))
        assertNull(detectYandexEntityType(""))
    }

    @Test fun `track regex catches mid-path track segment`() {
        val r = detectYandexEntityType("https://music.yandex.ru/album/1/tracks")
        // No `track/<digits>` segment, should not match TRACK_RE
        assertNull(r)
    }
}
