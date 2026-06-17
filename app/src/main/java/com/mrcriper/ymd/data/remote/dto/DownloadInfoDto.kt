package com.mrcriper.ymd.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class DownloadInfoResponseDto(
    val result: List<DownloadInfoDto> = emptyList(),
)

@Serializable
data class DownloadInfoDto(
    val codec: String? = null,
    val bitrate: Int? = null,
    val gain: Boolean? = null,
    val preview: Boolean? = null,
    val downloadInfo: DownloadInfoDetailDto? = null,
    val error: String? = null,
)

@Serializable
data class DownloadInfoDetailDto(
    val codec: String? = null,
    val bitrate: Int? = null,
    val quality: String? = null,
    val urls: List<String> = emptyList(),
    val key: String? = null,
    val salt: String? = null,
    val format: String? = null,
    val direct: Boolean? = null,
    val transport: String? = null,
    val fileSize: Long? = null,
)
