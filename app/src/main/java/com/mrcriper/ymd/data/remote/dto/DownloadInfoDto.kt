package com.mrcriper.ymd.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DownloadInfoResponseDto(
    val result: DownloadInfoResultDto? = null,
)

@Serializable
data class DownloadInfoResultDto(
    @SerialName("downloadInfo")
    val downloadInfo: DownloadInfoDto? = null,
    val name: String? = null,
    val message: String? = null,
)

@Serializable
data class DownloadInfoDto(
    @SerialName("trackId")
    val trackId: String? = null,
    val codec: String? = null,
    val bitrate: Int? = null,
    val gain: Boolean? = null,
    val preview: Boolean? = null,
    val quality: String? = null,
    val transport: String? = null,
    val key: String? = null,
    val urls: List<String> = emptyList(),
    val url: String? = null,
    val size: Long? = null,
    val error: String? = null,
)
