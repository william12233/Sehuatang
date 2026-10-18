package com.william12233.sehuatang.web.download

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DownloadViewModel(application: Application) : AndroidViewModel(application) {
    private val controller = DownloadController(application)
    private val _current = MutableStateFlow<DownloadProgress?>(null)
    val current = _current.asStateFlow()
    private var pollingJob: Job? = null

    fun enqueue(request: DownloadRequest): Result<Long> = runCatching {
        val id = controller.enqueue(request)
        startPolling(id)
        id
    }

    fun refresh(id: Long) {
        _current.value = controller.query(id)
    }

    fun openCurrent(): Boolean = _current.value?.let { controller.open(it.id) } == true

    fun dismiss() {
        _current.value = null
        pollingJob?.cancel()
    }

    fun cancelCurrent() {
        _current.value?.let { controller.remove(it.id) }
        _current.value = null
        pollingJob?.cancel()
    }

    fun onDownloadBroadcast(id: Long) {
        val progress = controller.query(id) ?: return
        if (controller.looksLikeHtml(progress)) {
            controller.remove(id)
            _current.value = progress.copy(status = DownloadStatus.FAILED)
            pollingJob?.cancel()
            return
        }
        _current.value = progress
        if (progress.status == DownloadStatus.SUCCESSFUL || progress.status == DownloadStatus.FAILED) {
            pollingJob?.cancel()
        }
    }

    private fun startPolling(id: Long) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                val progress = controller.query(id) ?: break
                _current.value = progress
                if (progress.status == DownloadStatus.SUCCESSFUL || progress.status == DownloadStatus.FAILED) {
                    if (progress.status == DownloadStatus.SUCCESSFUL && controller.looksLikeHtml(progress)) {
                        controller.remove(id)
                        _current.value = progress.copy(status = DownloadStatus.FAILED)
                    }
                    break
                }
                delay(500)
            }
        }
    }
}
