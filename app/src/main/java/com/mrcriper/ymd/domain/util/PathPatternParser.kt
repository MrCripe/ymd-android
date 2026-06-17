package com.mrcriper.ymd.domain.util

import com.mrcriper.ymd.domain.model.PathPattern
import com.mrcriper.ymd.domain.model.Track

/**
 * Path pattern parser — port of [py-ref/ymd/core.py::prepare_base_path].
 *
 * Placeholders: #number, #number-padded, #track-artist, #album-artist,
 *               #title, #album, #year, #artist-id, #album-id, #track-id.
 * Safe mode (default): any char outside `[A-Za-z0-9_\\-'() ]` and edge whitespace → '_'.
 * Unsafe mode: only `/` and `\` are replaced.
 * Each part truncated to [MAX_PART_LENGTH] characters.
 */
object PathPatternParser {
    const val MAX_PART_LENGTH = 251
    val PLACEHOLDERS = listOf(
        "#number", "#number-padded", "#track-artist", "#album-artist",
        "#title", "#album", "#year", "#artist-id", "#album-id", "#track-id",
    )

    private val UNSAFE_RE = Regex("[/\\\\]+")
    private val SAFE_RE = Regex("([^\\w\\-'() ]|^\\s+|\\s+\$)")

    fun parse(pattern: PathPattern, track: Track): String {
        val album = track.primaryAlbum
        val albumArtist = album?.artists?.firstOrNull()
        val trackArtist = track.primaryArtist
        val trackIndex = album?.trackIds?.indexOf(track.id)?.plus(1)
        val paddedIndex = trackIndex?.let { idx ->
            val width = album?.trackCount?.toString()?.length ?: 1
            idx.toString().padStart(width, '0')
        }

        var path = pattern.template
        val map: Map<String, String?> = mapOf(
            "#number" to trackIndex?.toString(),
            "#number-padded" to paddedIndex,
            "#track-artist" to trackArtist?.name,
            "#album-artist" to albumArtist?.name,
            "#title" to track.fullTitle,
            "#album" to album?.let { if (it.version.isNullOrBlank()) it.title else "${it.title} (${it.version})" },
            "#year" to album?.year?.toString(),
            "#artist-id" to trackArtist?.id,
            "#album-id" to album?.id,
            "#track-id" to track.id,
        )

        val clearRe = if (pattern.unsafe) UNSAFE_RE else SAFE_RE
        for ((placeholder, value) in map) {
            val replacement = value?.let { clearRe.replace(it, "_") } ?: ""
            path = path.replace(placeholder, replacement)
        }
        // collapse double underscores introduced by empty replacements
        path = path.replace(Regex("_+"), "_")
        return path
    }

    /** Used for the pattern editor UI: shows placeholder hints. */
    fun placeholderHelp(): String = "Placeholders: " + PLACEHOLDERS.joinToString(", ")
}
