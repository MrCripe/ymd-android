package com.mrcriper.ymd.domain.util

import com.mrcriper.ymd.domain.model.Container
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.images.StandardArtwork
import org.jaudiotagger.tag.mp4.Mp4FieldKey
import org.jaudiotagger.tag.mp4.Mp4Tag
import org.jaudiotagger.tag.mp4.field.Mp4TagCoverField
import java.io.File

data class CoverArt(val bytes: ByteArray, val mime: String) {
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

        when (container) {
            Container.MP4 -> {
                // For MP4/M4A: cast to Mp4Tag for proper MP4-specific API
                val mp4Tag = tag as? Mp4Tag ?: return
                mp4Tag.setField(Mp4FieldKey.TITLE, title)
                mp4Tag.setField(Mp4FieldKey.ALBUM, album)
                val joiner = if (compatibilityLevel >= 1) "; " else ", "
                mp4Tag.setField(Mp4FieldKey.ARTIST, artists.joinToString(joiner))
                runCatching { mp4Tag.setField(Mp4FieldKey.ALBUM_ARTIST, albumArtists.joinToString(joiner)) }
                trackNumber?.let { mp4Tag.setField(Mp4FieldKey.TRACK, it.toString()) }
                runCatching { discNumber?.let { mp4Tag.setField(Mp4FieldKey.DISCNUMBER, it.toString()) } }
                genre?.let { mp4Tag.setField(Mp4FieldKey.GENRE, it) }

                cover?.let { art ->
                    runCatching {
                        val coverField = Mp4TagCoverField(art.bytes) as org.jaudiotagger.tag.TagField
                        mp4Tag.setField(coverField)
                    }
                }
            }
            else -> {
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
                        val artwork = StandardArtwork()
                        artwork.binaryData = art.bytes
                        artwork.mimeType = art.mime
                        tag.setField(artwork)
                    }
                }

                when (container) {
                    Container.MP3 -> runCatching { tag.setField(FieldKey.URL_OFFICIAL_RELEASE_SITE, url) }
                    Container.FLAC -> runCatching { tag.setField(FieldKey.URL_OFFICIAL_RELEASE_SITE, url) }
                    else -> {}
                }
            }
        }

        AudioFileIO.write(audioFile)
    }
}
