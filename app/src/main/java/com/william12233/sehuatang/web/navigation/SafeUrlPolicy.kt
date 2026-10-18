package com.william12233.sehuatang.web.navigation

import java.net.URI

/**
 * 所有 WebView 導覽與本機歷史都經過這個小型相容層。
 * 未知 HTTPS 網域仍可在隔離的 WebView 中供人工驗證使用，但不會被視為論壇可信網域。
 */
object SafeUrlPolicy {
    private val trustedHosts = setOf("sehuatang.org", "www.sehuatang.org")

    private fun parse(url: String): URI? = runCatching { URI(url) }.getOrNull()

    fun isHttpUrl(url: String?): Boolean {
        val uri = url?.let(::parse) ?: return false
        return uri.scheme.equals("https", ignoreCase = true) ||
            uri.scheme.equals("http", ignoreCase = true)
    }

    fun isTrustedForumHost(url: String?): Boolean {
        val host = url?.let(::parse)?.host?.lowercase() ?: return false
        return host in trustedHosts || host.endsWith(".sehuatang.org")
    }

    fun isSensitiveUrl(url: String?): Boolean {
        val uri = url?.let(::parse) ?: return false
        val path = uri.path.orEmpty().lowercase()
        val query = uri.rawQuery.orEmpty().lowercase()
        val sensitiveWords = listOf(
            "password",
            "passwd",
            "pwd",
            "token",
            "secret",
            "auth",
            "formhash",
        )
        return path.contains("login") ||
            query.contains("action=login") ||
            query.contains("mod=logging") ||
            sensitiveWords.any(query::contains)
    }

    fun canPersistInLocalHistory(url: String?): Boolean =
        isHttpUrl(url) && isTrustedForumHost(url) && !isSensitiveUrl(url)

    fun displayHost(url: String?): String =
        url?.let(::parse)?.host.orEmpty().ifBlank { "網頁" }
}
