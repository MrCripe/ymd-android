package com.mrcriper.ymd.domain.model

data class Artist(
    val id: String,
    val name: String,
    val coverUri: String? = null,
    val various: Boolean = false,
    val available: Boolean = true,
)

data class Album(
    val id: String,
    val title: String,
    val version: String? = null,
    val coverUri: String? = null,
    val year: Int? = null,
    val trackCount: Int? = null,
    val metaType: String? = null,
    val available: Boolean = true,
    val artists: List<Artist> = emptyList(),
    val trackIds: List<String> = emptyList(),
    val genre: String? = null,
    val releaseDate: String? = null,
)

data class Track(
    val id: String,
    val title: String,
    val version: String? = null,
    val available: Boolean = true,
    val durationMs: Long? = null,
    val coverUri: String? = null,
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val hasTextLyrics: Boolean = false,
    val hasSyncLyrics: Boolean = false,
    val lyricsLanguage: String? = null,
) {
    val primaryAlbum: Album? get() = albums.firstOrNull()
    val primaryArtist: Artist? get() = artists.firstOrNull()
    val fullTitle: String get() = if (version.isNullOrBlank()) title else "$title ($version)"
    val trackNumber: Int? get() = primaryAlbum?.trackIds?.indexOf(id)?.plus(1)?.takeIf { it > 0 }
}

data class Playlist(
    val owner: String,
    val kind: String,
    val title: String? = null,
    val description: String? = null,
    val coverUri: String? = null,
    val trackCount: Int = 0,
    val trackIds: List<String> = emptyList(),
    val revision: Long? = null,
    val isPublic: Boolean = true,
)

data class PathPattern(
    val template: String = "#album-artist/#album/#number - #title",
    val unsafe: Boolean = false,
)

data class DownloadInfo(
    val trackId: String,
    val quality: DownloadQuality,
    val fileFormat: FileFormat,
    val urls: List<String>,
    val decryptionKey: String?,
    val bitrate: Int,
)

data class DownloadTask(
    val id: String,
    val track: Track,
    val info: DownloadInfo,
    val targetPath: String,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val bytesDownloaded: Long = 0,
    val totalBytes: Long = 0,
    val errorMessage: String? = null,
) {
    val progress: Float
        get() = if (totalBytes > 0) bytesDownloaded.toFloat() / totalBytes else 0f
}
