package com.mrcriper.ymd.domain.model

/**
 * Mirrors [py-ref/ymd/api.py::ApiTrackQuality] and [py-ref/ymd/core.py::CoreTrackQuality].
 * Maps user-friendly label → API value (`lq`/`nq`/`lossless`).
 */
enum class DownloadQuality(val value: Int, val apiValue: String, val label: String) {
    LOW(0, "lq", "Low (AAC 64)"),
    NORMAL(1, "nq", "Medium (AAC 192)"),
    BEST(2, "lossless", "Best (FLAC)");

    companion object {
        fun from(value: Int?): DownloadQuality = entries.firstOrNull { it.value == value } ?: BEST
    }
}

enum class LyricFormat(val label: String) {
    NONE("None"),
    TEXT("Text"),
    LRC("LRC");

    companion object {
        fun from(value: String?): LyricFormat = entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: NONE
    }
}

enum class Container(val extension: String, val label: String) {
    MP3(".mp3", "MP3"),
    FLAC(".flac", "FLAC"),
    MP4(".m4a", "M4A");

    companion object {
        fun fromLabel(label: String?): Container? = entries.firstOrNull { it.label.equals(label, true) }
    }
}

enum class DownloadStatus { QUEUED, RUNNING, PAUSED, COMPLETED, FAILED, CANCELLED }

data class FileFormat(val container: Container, val codec: String)
