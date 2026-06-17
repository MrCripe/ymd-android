package com.mrcriper.ymd.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrcriper.ymd.data.local.datastore.AppSettings
import com.mrcriper.ymd.data.remote.api.YandexEntity
import com.mrcriper.ymd.data.repository.SettingsRepository
import com.mrcriper.ymd.domain.model.DownloadQuality
import com.mrcriper.ymd.domain.model.LyricFormat
import com.mrcriper.ymd.domain.usecase.ParseUrlUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val url: String = "",
    val detectedType: YandexEntity? = null,
    val settings: AppSettings = AppSettings(),
    val isStarting: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val parseUrl: ParseUrlUseCase,
    private val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val url = MutableStateFlow(savedStateHandle.get<String>(KEY_URL).orEmpty())
    private val isStarting = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    val state: StateFlow<HomeUiState> = combine(
        url, isStarting, message, settingsRepository.settings,
    ) { u, running, msg, s ->
        HomeUiState(
            url = u,
            detectedType = parseUrl(u),
            settings = s,
            isStarting = running,
            message = msg,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun onUrlChange(value: String) {
        url.value = value
    }

    fun updateQuality(q: DownloadQuality) = viewModelScope.launch {
        settingsRepository.update { it.copy(quality = q) }
    }

    fun updateLyrics(l: LyricFormat) = viewModelScope.launch {
        settingsRepository.update { it.copy(lyricsFormat = l) }
    }

    fun update(block: (AppSettings) -> AppSettings) = viewModelScope.launch {
        settingsRepository.update(block)
    }

    fun startDownload() {
        val entity = state.value.detectedType ?: run {
            message.value = "Invalid URL or ID"
            return
        }
        isStarting.value = true
        message.value = "Queued: $entity"
        isStarting.value = false
    }

    fun consumeMessage() {
        message.value = null
    }

    companion object {
        const val KEY_URL = "url"
    }
}
