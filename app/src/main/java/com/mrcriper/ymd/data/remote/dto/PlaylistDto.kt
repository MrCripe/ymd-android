package com.mrcriper.ymd.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class PlaylistDto(
    val owner: PlaylistOwnerDto? = null,
    val uid: Long? = null,
    val kind: Long? = null,
    val title: String? = null,
    val description: String? = null,
    val cover: PlaylistCoverDto? = null,
    val coverUri: String? = null,
    val ogImage: String? = null,
    val trackCount: Int? = null,
    val durationMs: Long? = null,
    val isPublic: Boolean? = null,
    val available: Boolean? = null,
    val tracks: List<TrackShortDto> = emptyList(),
    val revision: Long? = null,
    val snapshot: Long? = null,
    val tags: List<TagDto> = emptyList(),
    val prerelease: Boolean? = null,
)

@Serializable
data class PlaylistOwnerDto(
    val uid: Long? = null,
    val login: String? = null,
    val name: String? = null,
    val sex: String? = null,
    val verified: Boolean? = null,
)

@Serializable
data class PlaylistCoverDto(
    val type: String? = null,
    val uri: String? = null,
    val prefix: String? = null,
    val itemsUri: List<String> = emptyList(),
)

@Serializable
data class TrackShortDto(
    val id: String? = null,
    val albumId: String? = null,
    val timestamp: Long? = null,
    val track: TrackDto? = null,
    val originalIndex: Int? = null,
)

@Serializable
data class TagDto(
    val id: String? = null,
    val value: String? = null,
)

@Serializable
data class PlaylistResponseDto(
    val result: PlaylistDto? = null,
)

@Serializable
data class PagerDto(
    val total: Int? = null,
    val page: Int? = null,
    val perPage: Int? = null,
)
