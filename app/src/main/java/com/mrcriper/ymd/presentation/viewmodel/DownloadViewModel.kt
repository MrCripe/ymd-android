package com.mrcriper.ymd.presentation.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class DownloadItem(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val bitrate: Int,
    val format: String,
    val progress: Float,
    val status: Status,
) {
    enum class Status { QUEUED, RUNNING, PAUSED, COMPLETED, FAILED, CANCELLED }
}

data class DownloadUiState(val items: List<DownloadItem> = emptyList())

@HiltViewModel
class DownloadViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(DownloadUiState())
    val state: StateFlow<DownloadUiState> = _state.asStateFlow()

    fun pause(id: String) = update(id) { it.copy(status = DownloadItem.Status.PAUSED) }
    fun resume(id: String) = update(id) { it.copy(status = DownloadItem.Status.RUNNING) }
    fun cancel(id: String) = update(id) { it.copy(status = DownloadItem.Status.CANCELLED) }
    fun cancelAll() {
        _state.update { s -> s.copy(items = s.items.map { it.copy(status = DownloadItem.Status.CANCELLED) }) }
    }

    private fun update(id: String, block: (DownloadItem) -> DownloadItem) {
        _state.update { s -> s.copy(items = s.items.map { if (it.id == id) block(it) else it }) }
    }
}
