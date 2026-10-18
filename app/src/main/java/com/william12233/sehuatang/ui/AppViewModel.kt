package com.william12233.sehuatang.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.william12233.sehuatang.data.model.LocalBookmark
import com.william12233.sehuatang.data.model.RecentVisit
import com.william12233.sehuatang.data.model.ThemeMode
import com.william12233.sehuatang.data.repository.LocalDataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LocalDataRepository(application)

    private val _recentVisits = MutableStateFlow<List<RecentVisit>>(emptyList())
    val recentVisits = _recentVisits.asStateFlow()

    private val _bookmarks = MutableStateFlow<List<LocalBookmark>>(emptyList())
    val bookmarks = _bookmarks.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode = _themeMode.asStateFlow()

    init {
        viewModelScope.launch {
            launch { repository.recentVisits.collect { _recentVisits.value = it } }
            launch { repository.bookmarks.collect { _bookmarks.value = it } }
            launch { repository.themeMode.collect { _themeMode.value = it } }
        }
    }

    fun recordVisit(title: String, url: String) {
        viewModelScope.launch { repository.recordVisit(title, url) }
    }

    fun isBookmarked(url: String?): Boolean =
        url?.let { _bookmarks.value.any { bookmark -> bookmark.url == it } } == true

    fun toggleBookmark(title: String, url: String) {
        viewModelScope.launch {
            if (isBookmarked(url)) {
                repository.removeBookmark(url)
            } else {
                repository.addBookmark(title, url)
            }
        }
    }

    fun removeBookmark(url: String) {
        viewModelScope.launch { repository.removeBookmark(url) }
    }

    fun clearRecentVisits() {
        viewModelScope.launch { repository.clearRecentVisits() }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }
}

/** 供 UI 渲染時使用的排序與顯示小工具。 */
fun List<RecentVisit>.latestVisitsFirst(): List<RecentVisit> = sortedByDescending { it.visitedAt }

fun List<LocalBookmark>.latestBookmarksFirst(): List<LocalBookmark> = sortedByDescending { it.savedAt }
