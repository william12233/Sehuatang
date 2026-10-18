package com.william12233.sehuatang.data.repository

import android.content.Context
import com.william12233.sehuatang.data.model.LocalBookmark
import com.william12233.sehuatang.data.model.RecentVisit
import com.william12233.sehuatang.data.model.ThemeMode
import com.william12233.sehuatang.web.navigation.SafeUrlPolicy
import org.json.JSONArray
import org.json.JSONObject

/** 只保存本機收藏、閱讀紀錄與外觀設定；不保存網站密碼或 Cookie。 */
class LocalDataRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "sehuatang_local_data",
        Context.MODE_PRIVATE,
    )

    fun readRecentVisits(): List<RecentVisit> = runCatching {
        val array = JSONArray(preferences.getString(KEY_RECENT, "[]"))
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(
                    RecentVisit(
                        title = item.optString("title").ifBlank { "論壇頁面" },
                        url = item.optString("url"),
                        visitedAt = item.optLong("visitedAt"),
                    ),
                )
            }
        }.filter { SafeUrlPolicy.canPersistInLocalHistory(it.url) }
    }.getOrDefault(emptyList())

    fun recordVisit(title: String, url: String) {
        if (!SafeUrlPolicy.canPersistInLocalHistory(url)) return
        val newItem = RecentVisit(
            title = title.ifBlank { "論壇頁面" }.take(120),
            url = url,
            visitedAt = System.currentTimeMillis(),
        )
        val updated = (listOf(newItem) + readRecentVisits().filterNot { it.url == url })
            .take(MAX_RECENT_ITEMS)
        writeRecent(updated)
    }

    fun readBookmarks(): List<LocalBookmark> = runCatching {
        val array = JSONArray(preferences.getString(KEY_BOOKMARKS, "[]"))
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(
                    LocalBookmark(
                        title = item.optString("title").ifBlank { "收藏頁面" },
                        url = item.optString("url"),
                        savedAt = item.optLong("savedAt"),
                    ),
                )
            }
        }.filter { SafeUrlPolicy.canPersistInLocalHistory(it.url) }
    }.getOrDefault(emptyList())

    fun isBookmarked(url: String): Boolean = readBookmarks().any { it.url == url }

    fun addBookmark(title: String, url: String) {
        if (!SafeUrlPolicy.canPersistInLocalHistory(url)) return
        val updated = (listOf(
            LocalBookmark(
                title = title.ifBlank { "收藏頁面" }.take(120),
                url = url,
                savedAt = System.currentTimeMillis(),
            ),
        ) + readBookmarks().filterNot { it.url == url }).take(MAX_BOOKMARKS)
        writeBookmarks(updated)
    }

    fun removeBookmark(url: String) {
        writeBookmarks(readBookmarks().filterNot { it.url == url })
    }

    fun clearRecentVisits() {
        preferences.edit().remove(KEY_RECENT).apply()
    }

    fun getThemeMode(): ThemeMode =
        runCatching {
            ThemeMode.valueOf(preferences.getString(KEY_THEME, ThemeMode.SYSTEM.name).orEmpty())
        }.getOrDefault(ThemeMode.SYSTEM)

    fun setThemeMode(mode: ThemeMode) {
        preferences.edit().putString(KEY_THEME, mode.name).apply()
    }

    private fun writeRecent(items: List<RecentVisit>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject()
                    .put("title", item.title)
                    .put("url", item.url)
                    .put("visitedAt", item.visitedAt),
            )
        }
        preferences.edit().putString(KEY_RECENT, array.toString()).apply()
    }

    private fun writeBookmarks(items: List<LocalBookmark>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject()
                    .put("title", item.title)
                    .put("url", item.url)
                    .put("savedAt", item.savedAt),
            )
        }
        preferences.edit().putString(KEY_BOOKMARKS, array.toString()).apply()
    }

    private companion object {
        const val KEY_RECENT = "recent_visits"
        const val KEY_BOOKMARKS = "local_bookmarks"
        const val KEY_THEME = "theme_mode"
        const val MAX_RECENT_ITEMS = 20
        const val MAX_BOOKMARKS = 100
    }
}
