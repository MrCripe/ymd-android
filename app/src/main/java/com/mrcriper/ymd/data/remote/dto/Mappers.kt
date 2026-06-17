package com.mrcriper.ymd.data.remote.dto

import com.mrcriper.ymd.domain.model.Album
import com.mrcriper.ymd.domain.model.Artist
import com.mrcriper.ymd.domain.model.Container
import com.mrcriper.ymd.domain.model.DownloadInfo
import com.mrcriper.ymd.domain.model.DownloadQuality
import com.mrcriper.ymd.domain.model.FileFormat
import com.mrcriper.ymd.domain.model.Playlist
import com.mrcriper.ymd.domain.model.Track

private val CODEC_FORMAT_MAP: Map<String, FileFormat> = mapOf(
    "flac" to FileFormat(Container.FLAC, "FLAC"),
    "flac-mp4" to FileFormat(Container.MP4, "FLAC"),
    "mp3" to FileFormat(Container.MP3, "MP3"),
    "aac" to FileFormat(Container.MP4, "AAC"),
    "he-aac" to FileFormat(Container.MP4, "AAC"),
    "aac-mp4" to FileFormat(Container.MP4, "AAC"),
    "he-aac-mp4" to FileFormat(Container.MP4, "AAC"),
)

fun ArtistDto.toArtist(): Artist = Artist(
    id = id ?: error("Artist id missing"),
    name = name.orEmpty(),
    coverUri = cover?.uri,
    various = false,
    available = true,
)

fun ArtistShortDto.toArtist(): Artist = Artist(
    id = id ?: error("Artist id missing"),
    name = name.orEmpty(),
    coverUri = cover?.uri,
    various = various ?: false,
    available = available ?: true,
)

fun AlbumDto.toAlbum(): Album = Album(
    id = id ?: error("Album id missing"),
    title = title.orEmpty(),
    version = version,
    coverUri = coverUri ?: ogImage,
    year = year,
    trackCount = trackCount,
    metaType = metaType,
    available = available ?: true,
    artists = artists.map { it.toArtist() },
    trackIds = trackIds.mapNotNull { it.id },
    genre = genre,
    releaseDate = releaseDate,
)

fun AlbumShortDto.toAlbum(): Album = Album(
    id = id ?: error("Album id missing"),
    title = title.orEmpty(),
    version = version,
    coverUri = coverUri ?: ogImage,
    year = year,
    trackCount = trackCount,
    metaType = metaType,
    available = available ?: true,
    artists = emptyList(),
)

fun AlbumWithTracksDto.toAlbum(): Album = Album(
    id = id ?: error("Album id missing"),
    title = title.orEmpty(),
    version = version,
    coverUri = coverUri,
    year = year,
    trackCount = trackCount ?: volumes.sumOf { it.size },
    metaType = metaType,
    available = available ?: true,
    artists = artists.map { it.toArtist() },
    trackIds = volumes.flatten().mapNotNull { it.id },
    genre = genre,
    releaseDate = releaseDate,
)

fun TrackDto.toTrack(): Track = Track(
    id = id ?: error("Track id missing"),
    title = title.orEmpty(),
    version = version,
    available = available ?: true,
    durationMs = durationMs,
    coverUri = coverUri ?: ogImage,
    albums = albums.map { it.toAlbum() },
    artists = artists.map { it.toArtist() },
    hasTextLyrics = lyricsInfo?.hasAvailableTextLyrics ?: false,
    hasSyncLyrics = lyricsInfo?.hasAvailableSyncLyrics ?: false,
    lyricsLanguage = lyricsInfo?.textLanguage,
)

fun DownloadInfoDto.toDownloadInfo(trackId: String, requested: DownloadQuality): DownloadInfo {
    val fmt = CODEC_FORMAT_MAP[codec]
        ?: error("Unknown codec from API: $codec")
    return DownloadInfo(
        trackId = trackId,
        quality = requested,
        fileFormat = fmt,
        urls = downloadInfo?.urls.orEmpty(),
        decryptionKey = downloadInfo?.key,
        bitrate = downloadInfo?.bitrate ?: bitrate ?: 0,
    )
}

fun AlbumDto.toAlbumWithTracks(): AlbumWithTracksDto = AlbumWithTracksDto(
    id = id,
    title = title,
    version = version,
    coverUri = coverUri ?: ogImage,
    trackCount = trackCount ?: trackIds.size,
    available = available,
    metaType = metaType,
    year = year,
    releaseDate = releaseDate,
    genre = genre,
    description = description,
    artists = artists,
    labels = labels,
    volumes = volumesFromTrackIds(trackIds),
)

private fun volumesFromTrackIds(ids: List<TrackIdDto>): List<List<TrackDto>> =
    if (ids.isEmpty()) emptyList() else listOf(ids.map { TrackDto(id = it.id) })

fun PlaylistDto.toPlaylist(owner: String): Playlist = Playlist(
    owner = owner,
    kind = kind?.toString().orEmpty(),
    title = title,
    description = description,
    coverUri = coverUri ?: cover?.uri,
    trackCount = trackCount ?: tracks.size,
    trackIds = tracks.mapNotNull { it.track?.id ?: it.id },
    revision = revision,
    isPublic = isPublic ?: true,
)
