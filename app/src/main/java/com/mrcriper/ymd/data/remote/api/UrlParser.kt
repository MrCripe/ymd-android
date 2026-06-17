package com.mrcriper.ymd.data.remote.api

import java.net.URI

/**
 * Yandex Music URL/ID detection and parsing.
 *
 * Patterns ported from [py-ref/ymd/cli.py]:
 *  - TRACK_RE    = track/(\d+)
 *  - ALBUM_RE    = album/(\d+)$
 *  - ARTIST_RE   = artist/(\d+)$
 *  - PLAYLIST_RE = ([\w\-._@]+)/playlists/(\d+)$
 */
sealed interface YandexEntity {
    data class Track(val trackId: String) : YandexEntity
    data class Album(val albumId: String) : YandexEntity
    data class Artist(val artistId: String) : YandexEntity
    data class Playlist(val owner: String, val kind: String) : YandexEntity
    data class RawId(val id: String, val kind: Kind) : YandexEntity
    enum class Kind { Track, Album, Artist, Playlist }
}

private val trackRe = Regex("""track/(\d+)""")
private val albumRe = Regex("""album/(\d+)$""")
private val artistRe = Regex("""artist/(\d+)$""")
private val playlistRe = Regex("""([\w\-._@]+)/playlists/(\d+)$""")

fun detectYandexEntityType(input: String): YandexEntity? {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return null
    if (!trimmed.contains('/') && !trimmed.contains('.')) {
        // bare ID — assume track by default
        return YandexEntity.RawId(trimmed, YandexEntity.Kind.Track)
    }
    val path = runCatching { URI(trimmed).path }.getOrNull() ?: trimmed

    playlistRe.find(path)?.let { m ->
        return YandexEntity.Playlist(m.groupValues[1], m.groupValues[2])
    }
    artistRe.find(path)?.let { m ->
        return YandexEntity.Artist(m.groupValues[1])
    }
    albumRe.find(path)?.let { m ->
        return YandexEntity.Album(m.groupValues[1])
    }
    trackRe.find(path)?.let { m ->
        return YandexEntity.Track(m.groupValues[1])
    }
    return null
}
