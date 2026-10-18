package com.william12233.sehuatang.data.remote

import com.william12233.sehuatang.data.model.ForumFailure
import com.william12233.sehuatang.data.model.ForumHome
import com.william12233.sehuatang.data.model.ForumMessage
import com.william12233.sehuatang.data.model.ForumConversation
import com.william12233.sehuatang.data.model.MessageReplyForm
import com.william12233.sehuatang.data.model.ForumResult
import com.william12233.sehuatang.data.model.ForumThreadPage
import com.william12233.sehuatang.data.model.LoginCredentials
import com.william12233.sehuatang.data.model.LoginForm
import com.william12233.sehuatang.data.model.PostForm
import com.william12233.sehuatang.data.model.SearchForm
import com.william12233.sehuatang.data.model.SearchResult
import com.william12233.sehuatang.data.model.UserProfile
import com.william12233.sehuatang.web.navigation.ForumUrls
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.jsoup.Jsoup

/** 只回傳已解析的資料模型或明確錯誤；不把 HTML 交給 UI。 */
class ForumDataSource(
    private val http: ForumHttpClient,
) {
    suspend fun loadHome(): ForumResult<ForumHome> =
        get(ForumUrls.HOME, ForumIndexParser::parse)

    suspend fun loadForumList(forumId: Long, page: Int = 1): ForumResult<ForumThreadPage> =
        get(ForumUrls.forumDisplay(forumId, page), ForumListParser::parse)

    suspend fun loadThread(url: String): ForumResult<ForumThreadPage> =
        get(url, ThreadDetailParser::parse)

    suspend fun search(query: String): ForumResult<SearchResult> {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return ForumResult.Success(SearchResult(emptyList(), sourceUrl = ForumUrls.SEARCH))
        return when (val formResult = loadSearchForm()) {
            is ForumResult.Failure -> formResult
            is ForumResult.Success -> {
                val form = formResult.value
                val fields = form.hiddenFields + (form.queryField to cleanQuery)
                val response = try {
                    withContext(Dispatchers.IO) {
                        if (form.method == "POST") {
                            http.postForm(form.action, fields, form.sourceUrl)
                        } else {
                            http.get(buildGetUrl(form.action, fields), form.sourceUrl)
                        }
                    }
                } catch (error: Exception) {
                    return ForumResult.Failure(
                        ForumFailure.Network(error.message ?: "搜尋網路連線失敗", error),
                    )
                }
                parseResponse(response, SearchParser::parse)
            }
        }
    }

    suspend fun loadSearchForm(): ForumResult<SearchForm> =
        get(ForumUrls.SEARCH, SearchParser::parseForm)

    suspend fun loadLoginForm(): ForumResult<LoginForm> =
        get(ForumUrls.LOGIN, LoginParser::parse)

    suspend fun login(credentials: LoginCredentials): ForumResult<Unit> {
        val formResult = loadLoginForm()
        if (formResult is ForumResult.Failure) return formResult
        val form = (formResult as ForumResult.Success).value
        val fields = form.hiddenFields.toMutableMap().apply {
            this[form.usernameField] = credentials.username
            this[form.passwordField] = credentials.password
            form.securityQuestionField?.let { field ->
                credentials.securityAnswer?.let { answer -> this[field] = answer }
            }
            if (credentials.rememberMe) {
                form.rememberField?.let { field -> this[field] = form.rememberValue ?: "1" }
            }
        }
        val response = try {
            withContext(Dispatchers.IO) { http.postForm(form.action, fields, form.sourceUrl) }
        } catch (error: Exception) {
            return ForumResult.Failure(ForumFailure.Network(error.message ?: "登入網路連線失敗", error))
        }
        if (ForumPageClassifier.verificationMessage(response.body) != null) {
            return ForumResult.Failure(
                ForumFailure.Verification(
                    response.finalUrl,
                    ForumPageClassifier.verificationMessage(response.body).orEmpty(),
                ),
            )
        }
        if (response.code == 401 || response.code == 403) {
            return ForumResult.Failure(ForumFailure.AuthenticationRequired)
        }
        if (response.code !in 200..299) {
            return ForumResult.Failure(ForumFailure.Http(response.code, "登入請求被伺服器拒絕"))
        }
        val bodyText = Jsoup.parse(response.body).text()
        val loginFailed = ForumPageClassifier.looksLikeLoginPage(response.body) ||
            Regex("登入失敗|密碼錯誤|帳號不存在|安全提問錯誤|login failed", RegexOption.IGNORE_CASE)
                .containsMatchIn(bodyText)
        return if (loginFailed) {
            ForumResult.Failure(ForumFailure.AuthenticationRequired)
        } else {
            ForumResult.Success(Unit)
        }
    }

    suspend fun loadReplyForm(threadUrl: String): ForumResult<PostForm> =
        get(threadUrl, PostFormParser::parse)

    suspend fun submitPost(
        form: PostForm,
        title: String? = null,
        content: String,
        quote: String? = null,
        files: List<ForumHttpClient.UploadPart> = emptyList(),
    ): ForumResult<Unit> {
        val fields = form.hiddenFields.toMutableMap().apply {
            if (!title.isNullOrBlank() && form.titleField != null) this[form.titleField] = title
            this[form.contentField] = content
            if (!quote.isNullOrBlank() && form.quoteField != null) this[form.quoteField] = quote
        }
        val response = try {
            withContext(Dispatchers.IO) {
                if (form.method == "GET") http.get(buildGetUrl(form.action, fields), form.sourceUrl)
                else if (files.isNotEmpty()) http.postMultipart(form.action, fields, files, form.sourceUrl)
                else http.postForm(form.action, fields, form.sourceUrl)
            }
        } catch (error: Exception) {
            return ForumResult.Failure(ForumFailure.Network(error.message ?: "送出文章時網路連線失敗", error))
        }
        return verifySubmission(response)
    }

    suspend fun loadMessages(): ForumResult<List<ForumMessage>> =
        get(ForumUrls.MESSAGES, MessageParser::parse)

    suspend fun loadConversation(url: String): ForumResult<ForumConversation> =
        get(url, MessageParser::parseConversation)

    suspend fun loadMessageReplyForm(url: String): ForumResult<MessageReplyForm> =
        get(url, MessageReplyFormParser::parse)

    suspend fun submitMessageReply(form: MessageReplyForm, content: String): ForumResult<Unit> {
        val fields = form.hiddenFields + (form.contentField to content)
        val response = try {
            withContext(Dispatchers.IO) {
                if (form.method == "GET") http.get(buildGetUrl(form.action, fields), form.sourceUrl)
                else http.postForm(form.action, fields, form.sourceUrl)
            }
        } catch (error: Exception) {
            return ForumResult.Failure(ForumFailure.Network(error.message ?: "送出私訊時網路連線失敗", error))
        }
        return verifySubmission(response)
    }

    suspend fun loadProfile(): ForumResult<UserProfile> =
        get(ForumUrls.MEMBER_HOME, ProfileParser::parse)

    fun cookieHeader(url: String): String? = http.cookieHeader(url)

    fun importVerificationCookies(url: String, cookieHeader: String?) =
        http.session.importCookieHeader(url, cookieHeader)

    fun clearSession() = http.clearSession()

    private suspend fun <T> get(
        url: String,
        parser: (String, String) -> ForumResult<T>,
    ): ForumResult<T> {
        val response = try {
            withContext(Dispatchers.IO) { http.get(url) }
        } catch (error: Exception) {
            return ForumResult.Failure(ForumFailure.Network(error.message ?: "論壇網路連線失敗", error))
        }
        return parseResponse(response, parser)
    }

    private fun <T> parseResponse(
        response: ForumHttpResponse,
        parser: (String, String) -> ForumResult<T>,
    ): ForumResult<T> {
        ForumPageClassifier.verificationMessage(response.body)?.let { message ->
            return ForumResult.Failure(ForumFailure.Verification(response.finalUrl, message))
        }
        if (response.code == 401) return ForumResult.Failure(ForumFailure.AuthenticationRequired)
        if (response.code == 403) {
            return ForumResult.Failure(ForumFailure.PermissionDenied("論壇拒絕了目前工作階段的權限"))
        }
        if (response.code !in 200..299) {
            return ForumResult.Failure(ForumFailure.Http(response.code, "論壇回傳 HTTP ${response.code}"))
        }
        return parser(response.body, response.finalUrl)
    }

    private fun verifySubmission(response: ForumHttpResponse): ForumResult<Unit> {
        ForumPageClassifier.verificationMessage(response.body)?.let { message ->
            return ForumResult.Failure(ForumFailure.Verification(response.finalUrl, message))
        }
        if (response.code == 401) return ForumResult.Failure(ForumFailure.AuthenticationRequired)
        if (response.code == 403) return ForumResult.Failure(ForumFailure.PermissionDenied("目前帳號沒有發文權限"))
        if (response.code !in 200..299) return ForumResult.Failure(ForumFailure.Http(response.code, "論壇未接受提交"))
        val document = Jsoup.parse(response.body, response.finalUrl)
        val message = document.select(".alert_error, .error, .notice, .tip_c, .warning").text().trim()
        if (message.isNotBlank() && Regex("錯誤|失敗|禁止|不足|驗證碼|error|failed", RegexOption.IGNORE_CASE).containsMatchIn(message)) {
            return ForumResult.Failure(ForumFailure.PermissionDenied(message.take(240)))
        }
        val successText = document.text()
        val confirmed = Regex("發表成功|回覆成功|操作成功|提交成功|succeed|success", RegexOption.IGNORE_CASE)
            .containsMatchIn(successText) || document.select("a[href*='viewthread'][href*='tid=']").isNotEmpty()
        return if (confirmed) ForumResult.Success(Unit)
        else ForumResult.Failure(ForumFailure.Parse("伺服器回應未包含可確認的送出結果，未顯示成功。"))
    }

    private fun buildGetUrl(action: String, fields: Map<String, String>): String {
        val base = action.toHttpUrlOrNull() ?: return action
        return base.newBuilder().apply {
            fields.forEach { (key, value) -> addQueryParameter(key, value) }
        }.build().toString()
    }
}
