package com.william12233.sehuatang.data.model

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

data class RecentVisit(
    val title: String,
    val url: String,
    val visitedAt: Long,
)

data class LocalBookmark(
    val title: String,
    val url: String,
    val savedAt: Long,
)
