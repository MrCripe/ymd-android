package com.mrcriper.ymd.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AlbumDto(
    val id: String? = null,
    val title: String? = null,
    val version: String? = null,
    val coverUri: String? = null,
    val ogImage: String? = null,
    val trackCount: Int? = null,
    val available: Boolean? = null,
    val metaType: String? = null,
    val year: Int? = null,
    val releaseDate: String? = null,
    val genre: String? = null,
    val description: String? = null,
    val trackPosition: TrackPositionDto? = null,
    val artists: List<ArtistShortDto> = emptyList(),
    val labels: List<LabelShortDto> = emptyList(),
    val regions: List<String> = emptyList(),
    val contentWarning: String? = null,
    val recent: Boolean? = null,
    val type: String? = null,
    val trackIds: List<TrackIdDto> = emptyList(),
)

@Serializable
data class AlbumShortDto(
    val id: String? = null,
    val title: String? = null,
    val version: String? = null,
    val coverUri: String? = null,
    val ogImage: String? = null,
    val trackCount: Int? = null,
    val trackPosition: TrackPositionDto? = null,
    val year: Int? = null,
    val releaseDate: String? = null,
    val type: String? = null,
    val metaType: String? = null,
    val available: Boolean? = null,
)

@Serializable
data class TrackIdDto(
    val id: String? = null,
    val albumId: String? = null,
)

@Serializable
data class AlbumsResponseDto(
    val result: List<AlbumDto> = emptyList(),
    val pager: PagerDto? = null,
)

@Serializable
data class AlbumWithTracksDto(
    val id: String? = null,
    val title: String? = null,
    val version: String? = null,
    val coverUri: String? = null,
    val trackCount: Int? = null,
    val available: Boolean? = null,
    val metaType: String? = null,
    val year: Int? = null,
    val releaseDate: String? = null,
    val genre: String? = null,
    val description: String? = null,
    val artists: List<ArtistShortDto> = emptyList(),
    val labels: List<LabelShortDto> = emptyList(),
    val volumes: List<List<TrackDto>> = emptyList(),
)
