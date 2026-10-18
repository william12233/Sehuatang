package com.william12233.sehuatang.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.william12233.sehuatang.data.model.LocalBookmark
import com.william12233.sehuatang.data.model.RecentVisit
import com.william12233.sehuatang.data.model.ThemeMode
import com.william12233.sehuatang.web.navigation.SafeUrlPolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.sehuatangDataStore by preferencesDataStore(name = "sehuatang_local_data")

/** 使用 DataStore 保存本機資料；不保存網站帳號或明文密碼。 */
class LocalDataRepository(context: Context) {
    private val dataStore = context.applicationContext.sehuatangDataStore

    val recentVisits: Flow<List<RecentVisit>> = dataStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { preferences ->
            decodeRecent(preferences[Keys.recent].orEmpty())
        }

    val bookmarks: Flow<List<LocalBookmark>> = dataStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { preferences ->
            decodeBookmarks(preferences[Keys.bookmarks].orEmpty())
        }

    val themeMode: Flow<ThemeMode> = dataStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { preferences ->
            runCatching { ThemeMode.valueOf(preferences[Keys.theme].orEmpty()) }
                .getOrDefault(ThemeMode.SYSTEM)
        }

    suspend fun recordVisit(title: String, url: String) {
        if (!SafeUrlPolicy.canPersistInLocalHistory(url)) return
        dataStore.edit { preferences ->
            val current = decodeRecent(preferences[Keys.recent].orEmpty())
            val item = RecentVisit(title.ifBlank { "論壇文章" }.take(120), url, System.currentTimeMillis())
            preferences[Keys.recent] = encodeRecent((listOf(item) + current.filterNot { it.url == url }).take(MAX_RECENT_ITEMS))
        }
    }

    suspend fun addBookmark(title: String, url: String) {
        if (!SafeUrlPolicy.canPersistInLocalHistory(url)) return
        dataStore.edit { preferences ->
            val current = decodeBookmarks(preferences[Keys.bookmarks].orEmpty())
            val item = LocalBookmark(title.ifBlank { "論壇文章" }.take(120), url, System.currentTimeMillis())
            preferences[Keys.bookmarks] = encodeBookmarks(
                (listOf(item) + current.filterNot { it.url == url }).take(MAX_BOOKMARKS),
            )
        }
    }

    suspend fun removeBookmark(url: String) {
        dataStore.edit { preferences ->
            preferences[Keys.bookmarks] = encodeBookmarks(decodeBookmarks(preferences[Keys.bookmarks].orEmpty()).filterNot { it.url == url })
        }
    }

    suspend fun clearRecentVisits() {
        dataStore.edit { it.remove(Keys.recent) }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.theme] = mode.name }
    }

    private fun decodeRecent(raw: String): List<RecentVisit> = runCatching {
        val array = JSONArray(raw.ifBlank { "[]" })
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(
                    RecentVisit(
                        title = item.optString("title").ifBlank { "論壇文章" },
                        url = item.optString("url"),
                        visitedAt = item.optLong("visitedAt"),
                    ),
                )
            }
        }.filter { SafeUrlPolicy.canPersistInLocalHistory(it.url) }
    }.getOrDefault(emptyList())

    private fun decodeBookmarks(raw: String): List<LocalBookmark> = runCatching {
        val array = JSONArray(raw.ifBlank { "[]" })
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(
                    LocalBookmark(
                        title = item.optString("title").ifBlank { "論壇文章" },
                        url = item.optString("url"),
                        savedAt = item.optLong("savedAt"),
                    ),
                )
            }
        }.filter { SafeUrlPolicy.canPersistInLocalHistory(it.url) }
    }.getOrDefault(emptyList())

    private fun encodeRecent(items: List<RecentVisit>): String = JSONArray().apply {
        items.forEach { item ->
            put(JSONObject().put("title", item.title).put("url", item.url).put("visitedAt", item.visitedAt))
        }
    }.toString()

    private fun encodeBookmarks(items: List<LocalBookmark>): String = JSONArray().apply {
        items.forEach { item ->
            put(JSONObject().put("title", item.title).put("url", item.url).put("savedAt", item.savedAt))
        }
    }.toString()

    private object Keys {
        val recent = stringPreferencesKey("recent_visits")
        val bookmarks = stringPreferencesKey("bookmarks")
        val theme = stringPreferencesKey("theme_mode")
    }

    private companion object {
        const val MAX_RECENT_ITEMS = 20
        const val MAX_BOOKMARKS = 100
    }
}
