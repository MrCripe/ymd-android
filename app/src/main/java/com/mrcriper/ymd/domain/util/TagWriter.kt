package com.mrcriper.ymd.domain.util

import com.mrcriper.ymd.domain.model.Container
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.ByteArrayInputStream
import java.io.File

data class CoverArt(val bytes: ByteArray, val mime: MimeType) {
    override fun equals(other: Any?): Boolean =
        this === other || (other is CoverArt && bytes.contentEquals(other.bytes) && mime == other.mime)
    override fun hashCode(): Int = bytes.contentHashCode() * 31 + mime.hashCode()
}

/**
 * Writes basic tags via jaudiotagger. Supports MP3/FLAC/MP4.
 * Tag map mirrors [py-ref/ymd/core.py::set_tags] for MP3 (ID3v2).
 */
class TagWriter {

    fun write(
        file: File,
        container: Container,
        codec: String = "",
        title: String,
        album: String,
        artists: List<String>,
        albumArtists: List<String>,
        trackNumber: Int?,
        discNumber: Int?,
        year: Int?,
        genre: String?,
        lyrics: String?,
        url: String,
        cover: CoverArt?,
        compatibilityLevel: Int = 1,
    ) {
        // flac-mp4 is FLAC audio inside MP4 container — jaudiotagger can't handle it
        if (container == Container.MP4 && codec == "FLAC") return

        val audioFile = try {
            AudioFileIO.read(file)
        } catch (e: Exception) {
            // Log the actual file header for debugging
            val header = file.readBytes().take(32).joinToString("") { "%02x".format(it) }
            throw Exception("jaudiotagger cannot read file (container=$container, codec=$codec). Header: $header", e)
        }
        val tag = audioFile.tag

        tag.setField(FieldKey.TITLE, title)
        tag.setField(FieldKey.ALBUM, album)
        val joiner = if (compatibilityLevel >= 1) "; " else ", "
        tag.setField(FieldKey.ARTIST, artists.joinToString(joiner))
        runCatching { tag.setField(FieldKey.ALBUM_ARTIST, albumArtists.joinToString(joiner)) }
        trackNumber?.let { tag.setField(FieldKey.TRACK, it.toString()) }
        runCatching { discNumber?.let { tag.setField(FieldKey.DISC_NO, it.toString()) } }
        year?.let { tag.setField(FieldKey.YEAR, it.toString()) }
        genre?.let { tag.setField(FieldKey.GENRE, it) }
        if (!lyrics.isNullOrBlank()) tag.setField(FieldKey.LYRICS, lyrics)

        cover?.let { art ->
            runCatching {
                tag.deleteArtworkField()
                val imageFile = File.createTempFile("cover", art.mime.extension)
                imageFile.writeBytes(art.bytes)
                imageFile.deleteOnExit()
                tag.setField(FieldKey.COVER_ART, imageFile.absolutePath)
            }
        }

        when (container) {
            Container.MP3 -> runCatching { tag.setField(FieldKey.URL_OFFICIAL_RELEASE_SITE, url) }
            Container.MP4 -> Unit
            Container.FLAC -> runCatching { tag.setField(FieldKey.URL_OFFICIAL_RELEASE_SITE, url) }
        }

        AudioFileIO.write(audioFile)
    }
}
