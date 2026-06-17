package com.mrcriper.ymd.domain.usecase

import com.mrcriper.ymd.data.remote.api.YandexEntity
import com.mrcriper.ymd.data.remote.api.YandexMusicApi
import com.mrcriper.ymd.data.remote.dto.ArtistDto
import com.mrcriper.ymd.data.remote.dto.TrackDto
import com.mrcriper.ymd.data.remote.dto.toAlbum
import com.mrcriper.ymd.data.remote.dto.toArtist
import com.mrcriper.ymd.data.remote.dto.toTrack
import com.mrcriper.ymd.domain.model.Album
import com.mrcriper.ymd.domain.model.Artist
import com.mrcriper.ymd.domain.model.Playlist
import com.mrcriper.ymd.domain.model.Track

class GetTrackInfoUseCase(private val api: YandexMusicApi) {
    suspend fun byId(trackId: String): Track? =
        api.getTracks(listOf(trackId)).firstOrNull()?.toTrack()

    suspend fun byEntity(entity: YandexEntity): List<Track> = when (entity) {
        is YandexEntity.Track -> api.getTracks(listOf(entity.trackId)).map { it.toTrack() }
        is YandexEntity.Album -> byAlbumId(entity.albumId)
        is YandexEntity.Artist -> byArtistId(entity.artistId)
        is YandexEntity.Playlist -> byPlaylist(entity.owner, entity.kind)
        is YandexEntity.RawId -> byId(entity.id).let(::listOfNotNull)
    }

    suspend fun byAlbumId(albumId: String): List<Track> {
        val album = api.getAlbumsWithTracks(albumId)
        return album.volumes.flatten().map { it.toTrack() }
    }

    suspend fun byArtistId(artistId: String): List<Track> {
        var page = 0
        val tracks = mutableListOf<TrackDto>()
        while (true) {
            val (albums, pager) = api.getArtistDirectAlbums(artistId, page)
            if (albums.isEmpty()) break
            for (album in albums) {
                val withTracks = api.getAlbumsWithTracks(album.id ?: continue)
                tracks += withTracks.volumes.flatten()
            }
            val total = pager?.total ?: 0
            val perPage = pager?.perPage ?: YandexMusicApi.FETCH_PAGE_SIZE
            if ((page + 1) * perPage >= total) break
            page++
        }
        return tracks.map { it.toTrack() }
    }

    suspend fun byPlaylist(owner: String, kind: String): List<Track> {
        val ids = api.getPlaylistTracks(owner, kind)
        return api.getTracks(ids).map { it.toTrack() }
    }

    suspend fun playlistMeta(owner: String, kind: String): Playlist? =
        api.getUserPlaylist(owner, kind)?.let { dto ->
            Playlist(
                owner = owner,
                kind = kind,
                title = dto.title,
                description = dto.description,
                coverUri = dto.coverUri ?: dto.cover?.uri,
                trackCount = dto.trackCount ?: 0,
                revision = dto.revision,
                isPublic = dto.isPublic ?: true,
            )
        }

    suspend fun artistMeta(artistId: String): Artist? =
        api.getArtist(artistId)?.let { it.toArtist() }

    suspend fun albumMeta(albumId: String): Album? =
        api.getAlbumsWithTracks(albumId).toAlbum()
}
