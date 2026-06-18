package com.mrcriper.ymd.presentation.viewmodel

import com.mrcriper.ymd.domain.model.DownloadQuality

data class DownloadRequest(
    val trackId: String,
    val quality: DownloadQuality,
    val saveDirUri: String?,
    val pathPattern: String,
)
