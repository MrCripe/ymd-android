package com.mrcriper.ymd.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ArtistDto(
    val id: String? = null,
    val name: String? = null,
    val cover: ArtistCoverDto? = null,
    val description: String? = null,
    val genres: List<String> = emptyList(),
    val counts: ArtistCountsDto? = null,
    val popularTracks: List<TrackDto> = emptyList(),
    val albums: List<AlbumShortDto> = emptyList(),
    val similar: List<ArtistShortDto> = emptyList(),
    val allCovers: ArtistCoversDto? = null,
    val customWave: String? = null,
    val ratings: RatingsDto? = null,
    val links: List<LinkDto> = emptyList(),
    val ticketsAvailable: Boolean? = null,
    val regions: List<String> = emptyList(),
)

@Serializable
data class ArtistShortDto(
    val id: String? = null,
    val name: String? = null,
    val cover: ArtistCoverDto? = null,
    val various: Boolean? = null,
    val composer: Boolean? = null,
    val available: Boolean? = null,
)

@Serializable
data class ArtistCoverDto(
    val type: String? = null,
    val uri: String? = null,
    val prefix: String? = null,
    val itemsUri: List<String> = emptyList(),
)

@Serializable
data class ArtistCoversDto(
    val type: String? = null,
    val uri: String? = null,
    val prefix: String? = null,
    val itemsUri: List<String> = emptyList(),
)

@Serializable
data class ArtistCountsDto(
    val tracks: Int? = null,
    val directAlbums: Int? = null,
    val alsoAlbums: Int? = null,
    val alsoTracks: Int? = null,
)

@Serializable
data class ArtistsResponseDto(
    val result: List<ArtistDto> = emptyList(),
    val pager: PagerDto? = null,
)

@Serializable
data class LabelShortDto(
    val id: String? = null,
    val name: String? = null,
)

@Serializable
data class RatingsDto(
    val month: Long? = null,
    val week: Long? = null,
    val day: Long? = null,
)

@Serializable
data class LinkDto(
    val title: String? = null,
    val href: String? = null,
    val type: String? = null,
    val socialNetwork: String? = null,
)
