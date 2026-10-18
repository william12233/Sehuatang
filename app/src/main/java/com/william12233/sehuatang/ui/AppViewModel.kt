package com.william12233.sehuatang.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.william12233.sehuatang.data.model.LocalBookmark
import com.william12233.sehuatang.data.model.RecentVisit
import com.william12233.sehuatang.data.model.ThemeMode
import com.william12233.sehuatang.data.repository.LocalDataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LocalDataRepository(application)

    private val _recentVisits = MutableStateFlow(repository.readRecentVisits())
    val recentVisits = _recentVisits.asStateFlow()

    private val _bookmarks = MutableStateFlow(repository.readBookmarks())
    val bookmarks = _bookmarks.asStateFlow()

    private val _themeMode = MutableStateFlow(repository.getThemeMode())
    val themeMode = _themeMode.asStateFlow()

    fun recordVisit(title: String, url: String) {
        repository.recordVisit(title, url)
        _recentVisits.value = repository.readRecentVisits()
    }

    fun isBookmarked(url: String?): Boolean =
        url?.let { _bookmarks.value.any { bookmark -> bookmark.url == it } } == true

    fun toggleBookmark(title: String, url: String) {
        if (isBookmarked(url)) {
            repository.removeBookmark(url)
        } else {
            repository.addBookmark(title, url)
        }
        _bookmarks.value = repository.readBookmarks()
    }

    fun removeBookmark(url: String) {
        repository.removeBookmark(url)
        _bookmarks.value = repository.readBookmarks()
    }

    fun clearRecentVisits() {
        repository.clearRecentVisits()
        _recentVisits.value = emptyList()
    }

    fun setThemeMode(mode: ThemeMode) {
        repository.setThemeMode(mode)
        _themeMode.value = mode
    }
}

/** 供 UI 渲染時使用的排序與顯示小工具。 */
fun List<RecentVisit>.latestVisitsFirst(): List<RecentVisit> = sortedByDescending { it.visitedAt }

fun List<LocalBookmark>.latestBookmarksFirst(): List<LocalBookmark> = sortedByDescending { it.savedAt }
