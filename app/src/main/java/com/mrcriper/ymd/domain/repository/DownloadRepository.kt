package com.mrcriper.ymd.domain.repository

import com.mrcriper.ymd.domain.model.DownloadTask
import com.mrcriper.ymd.domain.model.DownloadStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Singleton
import javax.inject.Inject

@Singleton
class DownloadRepository @Inject constructor() {

    private val _tasks = MutableStateFlow<Map<String, DownloadTask>>(emptyMap())
    val tasks: StateFlow<Map<String, DownloadTask>> = _tasks.asStateFlow()

    fun addTask(task: DownloadTask) {
        _tasks.update { it + (task.id to task) }
    }

    fun updateTask(id: String, updateBlock: (DownloadTask) -> DownloadTask) {
        _tasks.update { currentTasks ->
            val task = currentTasks[id] ?: return@update currentTasks
            currentTasks + (id to updateBlock(task))
        }
    }

    fun removeTask(id: String) {
        _tasks.update { it - id }
    }

    fun getTask(id: String): DownloadTask? = _tasks.value[id]
}
