package com.william12233.sehuatang.data.repository

import android.content.Context
import android.net.Uri
import com.william12233.sehuatang.data.model.ForumHome
import com.william12233.sehuatang.data.model.ForumMessage
import com.william12233.sehuatang.data.model.ForumConversation
import com.william12233.sehuatang.data.model.MessageReplyForm
import com.william12233.sehuatang.data.model.ForumResult
import com.william12233.sehuatang.data.model.ForumThreadPage
import com.william12233.sehuatang.data.model.LoginCredentials
import com.william12233.sehuatang.data.model.LoginForm
import com.william12233.sehuatang.data.model.PostForm
import com.william12233.sehuatang.data.model.SearchResult
import com.william12233.sehuatang.data.model.UserProfile
import com.william12233.sehuatang.data.remote.ForumDataSource
import com.william12233.sehuatang.data.remote.ForumHttpClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import android.provider.OpenableColumns

/** UI/ViewModel 與 HTTP/HTML 解析之間的唯一邊界。 */
class ForumRepository(context: Context) {
    private val appContext = context.applicationContext
    private val source = ForumDataSource(ForumHttpClient(context.applicationContext))

    suspend fun loadHome(): ForumResult<ForumHome> = source.loadHome()

    suspend fun loadForumList(forumId: Long, page: Int = 1): ForumResult<ForumThreadPage> =
        source.loadForumList(forumId, page)

    suspend fun loadThread(url: String): ForumResult<ForumThreadPage> = source.loadThread(url)

    suspend fun search(query: String): ForumResult<SearchResult> = source.search(query)

    suspend fun loadLoginForm(): ForumResult<LoginForm> = source.loadLoginForm()

    suspend fun login(credentials: LoginCredentials): ForumResult<Unit> = source.login(credentials)

    suspend fun loadReplyForm(threadUrl: String): ForumResult<PostForm> = source.loadReplyForm(threadUrl)

    suspend fun submitPost(
        form: PostForm,
        title: String?,
        content: String,
        quote: String?,
        attachments: List<Uri> = emptyList(),
    ): ForumResult<Unit> {
        if (attachments.isNotEmpty() && form.uploadField == null) {
            return ForumResult.Failure(
                com.william12233.sehuatang.data.model.ForumFailure.Parse(
                    "論壇回傳的發文表單沒有附件欄位，未送出所選檔案。",
                ),
            )
        }
        val files = attachments.mapNotNull { uri ->
            val name = displayName(uri) ?: "attachment"
            val body = UriRequestBody(uri)
            com.william12233.sehuatang.data.remote.ForumHttpClient.UploadPart(
                fieldName = form.uploadField ?: return@mapNotNull null,
                fileName = name,
                body = body,
            )
        }
        return source.submitPost(form, title, content, quote, files)
    }

    suspend fun loadMessages(): ForumResult<List<ForumMessage>> = source.loadMessages()

    suspend fun loadConversation(url: String): ForumResult<ForumConversation> = source.loadConversation(url)

    suspend fun loadMessageReplyForm(url: String): ForumResult<MessageReplyForm> = source.loadMessageReplyForm(url)

    suspend fun submitMessageReply(form: MessageReplyForm, content: String): ForumResult<Unit> =
        source.submitMessageReply(form, content)

    suspend fun loadProfile(): ForumResult<UserProfile> = source.loadProfile()

    fun cookieHeader(url: String): String? = source.cookieHeader(url)

    fun importVerificationCookies(url: String, cookieHeader: String?) =
        source.importVerificationCookies(url, cookieHeader)

    fun clearSession() = source.clearSession()

    private fun displayName(uri: Uri): String? = runCatching {
        appContext.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        } ?: uri.lastPathSegment?.substringAfterLast('/')
    }.getOrNull()

    private inner class UriRequestBody(private val uri: Uri) : RequestBody() {
        override fun contentType() = appContext.contentResolver.getType(uri)?.toMediaTypeOrNull()

        override fun contentLength(): Long = runCatching {
            appContext.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
        }.getOrDefault(-1L)

        override fun writeTo(sink: okio.BufferedSink) {
            appContext.contentResolver.openInputStream(uri)?.use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val count = input.read(buffer)
                    if (count <= 0) break
                    sink.write(buffer, 0, count)
                }
            } ?: error("無法讀取附件")
        }
    }
}
