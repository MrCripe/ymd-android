package com.mrcriper.ymd.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TrackDto(
    val id: String? = null,
    val title: String? = null,
    val version: String? = null,
    val available: Boolean? = null,
    val availableForPremium: Boolean? = null,
    val durationMs: Long? = null,
    val coverUri: String? = null,
    val ogImage: String? = null,
    val lyricsInfo: LyricInfoDto? = null,
    val albums: List<AlbumShortDto> = emptyList(),
    val artists: List<ArtistShortDto> = emptyList(),
    val contentRestricted: Boolean? = null,
    val type: String? = null,
    val trackSource: String? = null,
)

@Serializable
data class TracksResponseDto(
    val result: List<TrackDto> = emptyList(),
)

@Serializable
data class TrackPositionDto(
    val volume: Int? = null,
    val index: Int? = null,
)

@Serializable
data class LyricInfoDto(
    val hasAvailableTextLyrics: Boolean? = null,
    val hasAvailableSyncLyrics: Boolean? = null,
    val textLanguage: String? = null,
)

@Serializable
data class LyricsResponseDto(
    val result: TrackLyricsDto? = null,
)

@Serializable
data class TrackLyricsDto(
    val id: String? = null,
    val lyrics: String? = null,
    val fullLyrics: String? = null,
    val hasRights: Boolean? = null,
    val contributor: LyricsContributorDto? = null,
    val textLanguage: String? = null,
    val format: String? = null,
    val downloadUrl: String? = null,
)

@Serializable
data class LyricsContributorDto(
    val id: Long? = null,
    val name: String? = null,
    val avatar: String? = null,
)
