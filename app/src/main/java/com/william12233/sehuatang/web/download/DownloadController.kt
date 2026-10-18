package com.william12233.sehuatang.web.download

import android.app.DownloadManager
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.webkit.URLUtil
import com.william12233.sehuatang.web.navigation.SafeUrlPolicy
import java.util.Locale

data class DownloadRequest(
    val url: String,
    val userAgent: String?,
    val contentDisposition: String?,
    val mimeType: String?,
    val referer: String?,
    val cookie: String?,
)

enum class DownloadStatus {
    PENDING,
    RUNNING,
    PAUSED,
    SUCCESSFUL,
    FAILED,
    UNKNOWN,
}

data class DownloadProgress(
    val id: Long,
    val fileName: String,
    val status: DownloadStatus,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val mediaType: String?,
    val localUri: String?,
    val reason: Int,
) {
    val percent: Int?
        get() = if (totalBytes > 0) {
            ((downloadedBytes * 100L) / totalBytes).toInt().coerceIn(0, 100)
        } else {
            null
        }
}

/**
 * 以系統 DownloadManager 處理人工驗證後的短效下載連結。
 * Cookie 由原生 HTTP 工作階段按 URL 傳入，不做跨網域共用。
 */
class DownloadController(context: Context) {
    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(DownloadManager::class.java)
    private val resolver: ContentResolver = appContext.contentResolver

    fun enqueue(request: DownloadRequest): Long {
        require(SafeUrlPolicy.isHttpUrl(request.url)) { "只允許 HTTP(S) 下載" }

        val fileName = guessDownloadFileName(
            url = request.url,
            contentDisposition = request.contentDisposition,
            mimeType = request.mimeType,
        )
        val downloadRequest = DownloadManager.Request(Uri.parse(request.url))
            .setTitle(fileName)
            .setDescription("Sehuatang 附件下載")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

        request.mimeType?.takeIf { it.isNotBlank() }?.let(downloadRequest::setMimeType)
        request.userAgent?.takeIf { it.isNotBlank() }?.let {
            downloadRequest.addRequestHeader("User-Agent", it)
        }
        request.cookie?.takeIf { it.isNotBlank() }?.let {
            downloadRequest.addRequestHeader("Cookie", it)
        }
        request.referer?.takeIf { SafeUrlPolicy.isHttpUrl(it) }?.let {
            downloadRequest.addRequestHeader("Referer", it)
        }
        return manager.enqueue(downloadRequest)
    }

    fun query(id: Long): DownloadProgress? {
        val cursor = manager.query(DownloadManager.Query().setFilterById(id)) ?: return null
        cursor.use {
            if (!it.moveToFirst()) return null
            return it.toProgress(id)
        }
    }

    fun remove(id: Long): Boolean = manager.remove(id) > 0

    fun open(id: Long): Boolean {
        val progress = query(id) ?: return false
        if (progress.status != DownloadStatus.SUCCESSFUL) return false
        val uri = progress.localUri?.let(Uri::parse) ?: return false
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, progress.mediaType ?: "application/octet-stream")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            appContext.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    /** 不把反驗證 HTML 誤當成附件留在下載目錄。 */
    fun looksLikeHtml(progress: DownloadProgress): Boolean {
        if (progress.mediaType.orEmpty().lowercase(Locale.ROOT).contains("text/html")) return true
        val uri = progress.localUri?.let(Uri::parse) ?: return false
        return runCatching {
            resolver.openInputStream(uri)?.use { input ->
                val bytes = ByteArray(512)
                val count = input.read(bytes)
                if (count <= 0) return@runCatching false
                val prefix = bytes.copyOf(count).toString(Charsets.UTF_8).trimStart().lowercase()
                prefix.startsWith("<!doctype html") || prefix.startsWith("<html") ||
                    prefix.contains("<html")
            } ?: false
        }.getOrDefault(false)
    }

    private fun Cursor.toProgress(id: Long): DownloadProgress {
        val status = getInt(getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
        val fileName = getStringOrNull(DownloadManager.COLUMN_TITLE).orEmpty()
        return DownloadProgress(
            id = id,
            fileName = fileName,
            status = when (status) {
                DownloadManager.STATUS_PENDING -> DownloadStatus.PENDING
                DownloadManager.STATUS_RUNNING -> DownloadStatus.RUNNING
                DownloadManager.STATUS_PAUSED -> DownloadStatus.PAUSED
                DownloadManager.STATUS_SUCCESSFUL -> DownloadStatus.SUCCESSFUL
                DownloadManager.STATUS_FAILED -> DownloadStatus.FAILED
                else -> DownloadStatus.UNKNOWN
            },
            downloadedBytes = getLong(getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)),
            totalBytes = getLong(getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)),
            mediaType = getStringOrNull(DownloadManager.COLUMN_MEDIA_TYPE),
            localUri = getStringOrNull(DownloadManager.COLUMN_LOCAL_URI),
            reason = getInt(getColumnIndexOrThrow(DownloadManager.COLUMN_REASON)),
        )
    }

    private fun Cursor.getStringOrNull(column: String): String? {
        val index = getColumnIndex(column)
        return if (index >= 0 && !isNull(index)) getString(index) else null
    }
}

internal fun guessDownloadFileName(
    url: String,
    contentDisposition: String?,
    mimeType: String?,
): String {
    return sanitizeDownloadFileName(URLUtil.guessFileName(url, contentDisposition, mimeType))
}

internal fun sanitizeDownloadFileName(fileName: String): String {
    val sanitized = fileName
        .replace(Regex("[\\r\\n\\u0000-\\u001F]"), "_")
        .trim()
    return if (sanitized.isBlank() || sanitized.all { it == '_' }) {
        "sehuatang-download"
    } else {
        sanitized.take(180)
    }
}
