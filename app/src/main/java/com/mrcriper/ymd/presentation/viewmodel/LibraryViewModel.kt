package com.mrcriper.ymd.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrcriper.ymd.data.local.database.DownloadHistoryEntity
import com.mrcriper.ymd.data.repository.DownloadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LibraryUiState(
    val items: List<DownloadHistoryEntity> = emptyList(),
    val search: String = "",
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: DownloadRepository,
) : ViewModel() {

    private val search = MutableStateFlow("")

    val state: StateFlow<LibraryUiState> =
        combine(repository.observeRecent(), search) { items, q ->
            LibraryUiState(
                items = if (q.isBlank()) items else items.filter { it.title.contains(q, true) },
                search = q,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun onSearch(value: String) { search.value = value }

    fun delete(trackId: String) = viewModelScope.launch {
        repository.delete(trackId)
    }
}
