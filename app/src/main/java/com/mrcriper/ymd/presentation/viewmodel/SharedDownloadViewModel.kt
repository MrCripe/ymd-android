package com.mrcriper.ymd.presentation.viewmodel

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedDownloadViewModel @Inject constructor() {

    private val _downloadRequests = MutableSharedFlow<List<DownloadRequest>>(extraBufferCapacity = 1)
    val downloadRequests: SharedFlow<List<DownloadRequest>> = _downloadRequests.asSharedFlow()

    fun requestDownload(requests: List<DownloadRequest>) {
        _downloadRequests.tryEmit(requests)
    }
}
