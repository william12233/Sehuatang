package com.william12233.sehuatang.data.remote

import com.william12233.sehuatang.data.model.ContentBlock
import com.william12233.sehuatang.data.model.CheckInForm
import com.william12233.sehuatang.data.model.CheckInOutcome
import com.william12233.sehuatang.data.model.ForumAttachment
import com.william12233.sehuatang.data.model.ForumHome
import com.william12233.sehuatang.data.model.ForumMessage
import com.william12233.sehuatang.data.model.ForumConversation
import com.william12233.sehuatang.data.model.ConversationMessage
import com.william12233.sehuatang.data.model.MessageReplyForm
import com.william12233.sehuatang.data.model.ForumPost
import com.william12233.sehuatang.data.model.ForumResult
import com.william12233.sehuatang.data.model.ForumSection
import com.william12233.sehuatang.data.model.ForumThread
import com.william12233.sehuatang.data.model.ForumThreadPage
import com.william12233.sehuatang.data.model.LoginForm
import com.william12233.sehuatang.data.model.PostForm
import com.william12233.sehuatang.data.model.QuotedPost
import com.william12233.sehuatang.data.model.SearchResult
import com.william12233.sehuatang.data.model.SearchForm
import com.william12233.sehuatang.data.model.UserProfile
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import java.net.URI

/** 只辨識站點回傳的驗證頁，不嘗試執行、破解或繞過驗證腳本。 */
object ForumPageClassifier {
    fun verificationMessage(html: String): String? {
        val document = Jsoup.parse(html)
        val safeAsset = document.select("link[href*=static/safe], script[src*=static/safe]").isNotEmpty()
        val safeId = document.select("script").any { it.data().contains("safeid") }
        return if (safeAsset || safeId) {
            "論壇要求完成站點驗證後才能提供資料。請在隔離驗證頁由你本人完成驗證，再重試。"
        } else {
            null
        }
    }

    fun looksLikeLoginPage(html: String): Boolean {
        val document = Jsoup.parse(html)
        return document.select("form").any { form ->
            form.select("input[type=password]").isNotEmpty()
        }
    }
}

object ForumIndexParser {
    fun parse(html: String, sourceUrl: String): ForumResult<ForumHome> {
        if (ForumPageClassifier.verificationMessage(html) != null) {
            return ForumResult.Failure(
                com.william12233.sehuatang.data.model.ForumFailure.Verification(
                    sourceUrl,
                    ForumPageClassifier.verificationMessage(html).orEmpty(),
                ),
            )
        }
        val document = Jsoup.parse(html, sourceUrl)
        val sections = ParserSupport.parseSections(document)
        val threads = ParserSupport.parseThreads(document)
        if (sections.isEmpty() && threads.isEmpty()) {
            return ForumResult.Failure(
                com.william12233.sehuatang.data.model.ForumFailure.Parse(
                    "首頁沒有找到可辨識的版區或文章節點；保留原始網址供診斷：$sourceUrl",
                ),
            )
        }
        return ForumResult.Success(
            ForumHome(
                sections = sections,
                latestThreads = threads,
                popularThreads = threads.sortedByDescending { it.replies ?: 0 },
                sourceUrl = sourceUrl,
            ),
        )
    }
}

object ForumListParser {
    fun parse(html: String, sourceUrl: String): ForumResult<ForumThreadPage> {
        ForumPageClassifier.verificationMessage(html)?.let { message ->
            return ForumResult.Failure(
                com.william12233.sehuatang.data.model.ForumFailure.Verification(sourceUrl, message),
            )
        }
        val document = Jsoup.parse(html, sourceUrl)
        val threads = ParserSupport.parseThreads(document)
        if (threads.isEmpty()) {
            return ForumResult.Failure(
                com.william12233.sehuatang.data.model.ForumFailure.Parse(
                    "版區頁沒有找到文章列表節點；Parser 未將網站 HTML 當作畫面顯示。",
                ),
            )
        }
        val page = ParserSupport.currentPage(document)
        return ForumResult.Success(
            ForumThreadPage(
                thread = threads.first(),
                posts = emptyList(),
                threads = threads,
                page = page,
                totalPages = ParserSupport.totalPages(document),
                previousPageUrl = ParserSupport.pageLink(document, previous = true),
                nextPageUrl = ParserSupport.pageLink(document, previous = false),
                sourceUrl = sourceUrl,
            ),
        )
    }
}

object ThreadListParser {
    fun parse(html: String, sourceUrl: String): ForumResult<List<ForumThread>> {
        ForumPageClassifier.verificationMessage(html)?.let { message ->
            return ForumResult.Failure(
                com.william12233.sehuatang.data.model.ForumFailure.Verification(sourceUrl, message),
            )
        }
        val document = Jsoup.parse(html, sourceUrl)
        val threads = ParserSupport.parseThreads(document)
        return if (threads.isEmpty()) {
            ForumResult.Failure(
                com.william12233.sehuatang.data.model.ForumFailure.Parse(
                    "沒有找到可辨識的文章列表節點。",
                ),
            )
        } else {
            ForumResult.Success(threads)
        }
    }
}

object ThreadDetailParser {
    fun parse(html: String, sourceUrl: String): ForumResult<ForumThreadPage> {
        ForumPageClassifier.verificationMessage(html)?.let { message ->
            return ForumResult.Failure(
                com.william12233.sehuatang.data.model.ForumFailure.Verification(sourceUrl, message),
            )
        }
        val document = Jsoup.parse(html, sourceUrl)
        val postElements = document.select(
            "div[id^=post_], li[id^=pid], div.plhin, article.post, .post-item",
        )
        val posts = postElements.mapNotNull { parsePost(it, sourceUrl) }
        if (posts.isEmpty()) {
            return ForumResult.Failure(
                com.william12233.sehuatang.data.model.ForumFailure.Parse(
                    "文章頁沒有找到可辨識的帖子節點。",
                ),
            )
        }
        val title = document.selectFirst("h1, h2.ts, .thread-title, #thread_subject")
            ?.text()
            ?.trim()
            ?.ifBlank { null }
            ?: document.title().trim().ifBlank { "論壇文章" }
        val threadId = ParserSupport.queryLong(sourceUrl, "tid")
            ?: ParserSupport.queryLong(
                document.selectFirst("a[href*='mod=viewthread'][href*='tid=']")?.absUrl("href"),
                "tid",
            )
            ?: 0L
        val thread = ForumThread(
            id = threadId,
            forumId = ParserSupport.queryLong(sourceUrl, "fid"),
            title = title,
            author = posts.first().author,
            authorAvatar = posts.first().authorAvatar,
            createdAt = posts.first().createdAt,
            replies = (posts.size - 1).coerceAtLeast(0),
            url = sourceUrl,
        )
        return ForumResult.Success(
            ForumThreadPage(
                thread = thread,
                posts = posts,
                page = ParserSupport.currentPage(document),
                totalPages = ParserSupport.totalPages(document),
                previousPageUrl = ParserSupport.pageLink(document, previous = true),
                nextPageUrl = ParserSupport.pageLink(document, previous = false),
                sourceUrl = sourceUrl,
            ),
        )
    }

    private fun parsePost(element: Element, sourceUrl: String): ForumPost? {
        val id = ParserSupport.numericId(element.id(), "post_")
            ?: ParserSupport.numericId(element.id(), "pid")
            ?: ParserSupport.queryLong(element.selectFirst("a[href*='pid=']")?.absUrl("href"), "pid")
            ?: return null
        val authorElement = element.selectFirst(
            ".authi a[href*='space'], .authi a[href*='uid'], .author, .username, .pi a",
        )
        val author = authorElement?.text()?.trim().orEmpty().ifBlank { "論壇會員" }
        val avatar = element.selectFirst("img.avatar, .avatar img, .avt img, .psta img")?.absUrl("src")
        val createdAt = element.selectFirst(".authi em, .dateline, .pti .authi, .post-time")
            ?.text()
            ?.trim()
            .orEmpty()
        val content = element.selectFirst(".t_f, .pcb, .message, .post-content, .content") ?: element
        val attachments = ParserSupport.parseAttachments(content)
        val blocks = ParserSupport.parseContentBlocks(content, sourceUrl, attachments)
        val quote = content.selectFirst("blockquote, .quote, .blockquotemessage")?.let {
            QuotedPost(
                author = it.selectFirst("cite, .quote-author")?.text()?.trim(),
                text = it.text().trim(),
                postId = ParserSupport.queryLong(it.selectFirst("a[href*='pid=']")?.absUrl("href"), "pid"),
            )
        }
        val floor = element.selectFirst(".pi .authi em, .floor, .post-number")?.text()?.trim().orEmpty()
        val edit = element.selectFirst(".pdb, .edit-info, .edited")?.text()?.trim()
        return ForumPost(
            id = id,
            threadId = ParserSupport.queryLong(sourceUrl, "tid") ?: 0L,
            author = author,
            authorAvatar = avatar,
            floor = floor,
            createdAt = createdAt,
            contentBlocks = blocks,
            attachments = attachments,
            quotedPost = quote,
            editInformation = edit,
        )
    }
}

object SearchParser {
    fun parseForm(html: String, sourceUrl: String): ForumResult<SearchForm> {
        ForumPageClassifier.verificationMessage(html)?.let { message ->
            return ForumResult.Failure(
                com.william12233.sehuatang.data.model.ForumFailure.Verification(sourceUrl, message),
            )
        }
        val document = Jsoup.parse(html, sourceUrl)
        val form = document.select("form").firstOrNull { candidate ->
            candidate.select("input[name], textarea[name]").any { input ->
                val name = input.attr("name")
                name.contains("search", true) || name.contains("srchtxt", true) || name.contains("keyword", true)
            }
        } ?: return ForumResult.Failure(
            com.william12233.sehuatang.data.model.ForumFailure.Parse("搜尋頁沒有找到查詢表單。"),
        )
        val query = form.select("input[name], textarea[name]").firstOrNull { input ->
            val name = input.attr("name")
            name.contains("search", true) || name.contains("srchtxt", true) || name.contains("keyword", true)
        }?.attr("name") ?: return ForumResult.Failure(
            com.william12233.sehuatang.data.model.ForumFailure.Parse("搜尋表單沒有可用的查詢欄位。"),
        )
        return ForumResult.Success(
            SearchForm(
                action = form.absUrl("action").ifBlank { sourceUrl },
                method = form.attr("method").ifBlank { "GET" }.uppercase(),
                hiddenFields = form.select("input[type=hidden][name]")
                    .associate { it.attr("name") to it.attr("value") },
                queryField = query,
                sourceUrl = sourceUrl,
            ),
        )
    }

    fun parse(html: String, sourceUrl: String): ForumResult<SearchResult> {
        ForumPageClassifier.verificationMessage(html)?.let { message ->
            return ForumResult.Failure(
                com.william12233.sehuatang.data.model.ForumFailure.Verification(sourceUrl, message),
            )
        }
        val document = Jsoup.parse(html, sourceUrl)
        val threads = ParserSupport.parseThreads(document)
        if (threads.isEmpty()) {
            return ForumResult.Failure(
                com.william12233.sehuatang.data.model.ForumFailure.Parse("搜尋回應沒有找到文章結果節點。"),
            )
        }
        return ForumResult.Success(
            SearchResult(
                threads = threads,
                page = ParserSupport.currentPage(document),
                totalPages = ParserSupport.totalPages(document),
                nextPageUrl = ParserSupport.pageLink(document, previous = false),
                sourceUrl = sourceUrl,
            ),
        )
    }
}

object LoginParser {
    fun parse(html: String, sourceUrl: String): ForumResult<LoginForm> {
        ForumPageClassifier.verificationMessage(html)?.let { message ->
            return ForumResult.Failure(
                com.william12233.sehuatang.data.model.ForumFailure.Verification(sourceUrl, message),
            )
        }
        val document = Jsoup.parse(html, sourceUrl)
        val form = document.select("form").firstOrNull { it.select("input[type=password]").isNotEmpty() }
            ?: return ForumResult.Failure(
                com.william12233.sehuatang.data.model.ForumFailure.Parse("登入頁沒有找到密碼欄位。"),
            )
        val password = form.selectFirst("input[type=password][name]")?.attr("name")
            ?: return ForumResult.Failure(
                com.william12233.sehuatang.data.model.ForumFailure.Parse("登入表單沒有可用的密碼欄位名稱。"),
            )
        val username = form.selectFirst(
            "input[type=text][name], input[type=email][name], input[name*=user i][name]",
        )?.attr("name")
            ?: return ForumResult.Failure(
                com.william12233.sehuatang.data.model.ForumFailure.Parse("登入表單沒有可用的帳號欄位名稱。"),
            )
        val hidden = form.select("input[type=hidden][name]").associate { it.attr("name") to it.attr("value") }
        val security = form.selectFirst(
            "input[name*=question i][name], input[name*=answer i][name], input[name*=security i][name]",
        )?.attr("name")
        val remember = form.selectFirst(
            "input[type=checkbox][name*=remember i], input[type=checkbox][name*=cookietime i]",
        )?.attr("name")
        val rememberValue = remember?.let { name -> form.selectFirst("input[name='$name']")?.attr("value") }
        val captcha = form.selectFirst("img[src*=captcha], .captcha, [class*=captcha i]")?.text()?.trim()
            ?.ifBlank { "論壇要求驗證碼" }
        return ForumResult.Success(
            LoginForm(
                action = form.absUrl("action").ifBlank { sourceUrl },
                hiddenFields = hidden,
                usernameField = username,
                passwordField = password,
                securityQuestionField = security,
                rememberField = remember,
                rememberValue = rememberValue,
                captchaDescription = captcha,
                sourceUrl = sourceUrl,
            ),
        )
    }
}

object PostFormParser {
    fun parse(html: String, sourceUrl: String): ForumResult<PostForm> {
        ForumPageClassifier.verificationMessage(html)?.let { message ->
            return ForumResult.Failure(
                com.william12233.sehuatang.data.model.ForumFailure.Verification(sourceUrl, message),
            )
        }
        val document = Jsoup.parse(html, sourceUrl)
        val form = document.select("form").firstOrNull { candidate ->
            candidate.select("textarea[name]").isNotEmpty() &&
                candidate.select("input[type=hidden][name]").isNotEmpty()
        } ?: return ForumResult.Failure(
            com.william12233.sehuatang.data.model.ForumFailure.Parse("沒有找到可用的論壇發文表單。"),
        )
        val content = form.selectFirst("textarea[name], [contenteditable=true][name]")?.attr("name")
            ?: return ForumResult.Failure(
                com.william12233.sehuatang.data.model.ForumFailure.Parse("發文表單沒有正文欄位名稱。"),
            )
        val hidden = form.select("input[type=hidden][name]").associate { it.attr("name") to it.attr("value") }
        val title = form.selectFirst(
            "input[name*=subject i][name], input[name*=title i][name], input[type=text][name]",
        )?.attr("name")
        val quote = form.selectFirst("input[name*=quote i][name], textarea[name*=quote i][name]")?.attr("name")
        val upload = form.selectFirst(
            "input[type=file][name], input[name*=attach i][name], input[name*=upload i][name]",
        )?.attr("name")
        val captcha = form.selectFirst("img[src*=captcha], .captcha, [class*=captcha i]") != null
        return ForumResult.Success(
            PostForm(
                action = form.absUrl("action").ifBlank { sourceUrl },
                method = form.attr("method").ifBlank { "POST" }.uppercase(),
                hiddenFields = hidden,
                titleField = title,
                contentField = content,
                quoteField = quote,
                uploadField = upload,
                sourceUrl = sourceUrl,
                requiresCaptcha = captcha,
            ),
        )
    }
}

object CheckInParser {
    fun parse(html: String, sourceUrl: String): ForumResult<CheckInForm> {
        ForumPageClassifier.verificationMessage(html)?.let { message ->
            return ForumResult.Failure(
                com.william12233.sehuatang.data.model.ForumFailure.Verification(sourceUrl, message),
            )
        }
        if (ForumPageClassifier.looksLikeLoginPage(html)) {
            return ForumResult.Failure(com.william12233.sehuatang.data.model.ForumFailure.AuthenticationRequired)
        }
        val document = Jsoup.parse(html, sourceUrl)
        val form = document.select("form").firstOrNull { candidate ->
            val action = candidate.absUrl("action")
            val controls = candidate.select("input, button").joinToString(" ") {
                "${it.attr("name")} ${it.attr("value")} ${it.text()}"
            }
            val descriptor = "${action} ${candidate.text()} $controls".lowercase()
            action.contains("dd_sign", ignoreCase = true) ||
                descriptor.contains("簽到") ||
                descriptor.contains("签到") ||
                descriptor.contains("check-in") ||
                descriptor.contains("checkin")
        } ?: return ForumResult.Failure(
            com.william12233.sehuatang.data.model.ForumFailure.Parse(
                "簽到頁沒有找到可辨識的提交表單；未猜測任何欄位。",
            ),
        )
        val fields = linkedMapOf<String, String>()
        form.select("input[type=hidden][name], input[type=submit][name], button[type=submit][name]")
            .forEach { input -> fields[input.attr("name")] = input.attr("value") }
        val submitLabel = form.selectFirst("input[type=submit], button[type=submit]")
            ?.let { it.attr("value").ifBlank { it.text() } }
            ?.trim()
            .orEmpty()
            .ifBlank { "送出簽到" }
        return ForumResult.Success(
            CheckInForm(
                action = form.absUrl("action").ifBlank { sourceUrl },
                method = form.attr("method").ifBlank { "POST" }.uppercase(),
                fields = fields,
                label = submitLabel,
                requiresCaptcha = form.select("img[src*=captcha], .captcha, [class*=captcha i]").isNotEmpty(),
                sourceUrl = sourceUrl,
            ),
        )
    }

    fun parseSubmission(html: String, sourceUrl: String): ForumResult<CheckInOutcome> {
        ForumPageClassifier.verificationMessage(html)?.let { message ->
            return ForumResult.Failure(
                com.william12233.sehuatang.data.model.ForumFailure.Verification(sourceUrl, message),
            )
        }
        if (ForumPageClassifier.looksLikeLoginPage(html)) {
            return ForumResult.Failure(com.william12233.sehuatang.data.model.ForumFailure.AuthenticationRequired)
        }
        val document = Jsoup.parse(html, sourceUrl)
        val bodyText = document.text().trim()
        val errorText = document.select(".alert_error, .error, .notice, .tip_c, .warning").text().trim()
        if (errorText.isNotBlank() && Regex("錯誤|失败|失敗|禁止|不足|驗證碼|error|failed", RegexOption.IGNORE_CASE)
                .containsMatchIn(errorText)
        ) {
            return ForumResult.Failure(
                com.william12233.sehuatang.data.model.ForumFailure.PermissionDenied(errorText.take(240)),
            )
        }
        val success = Regex(
            "簽到成功|签到成功|已簽到|已签到|恭喜[^。.!！]{0,40}(?:簽到|签到)|check[- ]?in[^.]{0,40}success|success",
            RegexOption.IGNORE_CASE,
        ).find(bodyText)?.value
        return if (success != null) {
            ForumResult.Success(CheckInOutcome(success.take(240)))
        } else {
            ForumResult.Failure(
                com.william12233.sehuatang.data.model.ForumFailure.Parse(
                    "伺服器回應未包含可確認的簽到成功結果，未顯示成功。",
                ),
            )
        }
    }
}

object MessageParser {
    fun parse(html: String, sourceUrl: String): ForumResult<List<ForumMessage>> {
        ForumPageClassifier.verificationMessage(html)?.let { message ->
            return ForumResult.Failure(
                com.william12233.sehuatang.data.model.ForumFailure.Verification(sourceUrl, message),
            )
        }
        val document = Jsoup.parse(html, sourceUrl)
        val messages = document.select(
            "li.pm_list, li[id*=pm], .pm-list-item, .message-item, .mbm",
        ).mapNotNull { item ->
            val url = item.selectFirst("a[href]")?.absUrl("href") ?: return@mapNotNull null
            val id = ParserSupport.queryLong(url, "pmid")
                ?: ParserSupport.numericId(item.id(), "pm_")
                ?: return@mapNotNull null
            ForumMessage(
                id = id,
                conversationId = ParserSupport.queryString(url, "plid") ?: id.toString(),
                author = item.selectFirst(".author, .username, .pm_name, a[href*=space]")?.text()?.trim()
                    .orEmpty()
                    .ifBlank { "論壇會員" },
                authorAvatar = item.selectFirst("img")?.absUrl("src"),
                preview = item.selectFirst(".summary, .message, .pm_content, p")?.text()?.trim()
                    .orEmpty(),
                createdAt = item.selectFirst(".time, .dateline, em")?.text()?.trim().orEmpty(),
                isRead = !item.classNames().any { it.contains("unread", ignoreCase = true) },
                url = url,
            )
        }
        return ForumResult.Success(messages)
    }

    fun parseConversation(html: String, sourceUrl: String): ForumResult<ForumConversation> {
        ForumPageClassifier.verificationMessage(html)?.let { message ->
            return ForumResult.Failure(
                com.william12233.sehuatang.data.model.ForumFailure.Verification(sourceUrl, message),
            )
        }
        if (ForumPageClassifier.looksLikeLoginPage(html)) {
            return ForumResult.Failure(com.william12233.sehuatang.data.model.ForumFailure.AuthenticationRequired)
        }
        val document = Jsoup.parse(html, sourceUrl)
        val messages = document.select(
            "[id^=pm_], [id^=message_], .pm_item, .pm-message, .message-item, .pm_message",
        ).mapIndexedNotNull { index, item ->
            val id = ParserSupport.numericId(item.id(), "pm_")
                ?: ParserSupport.numericId(item.id(), "message_")
                ?: ParserSupport.queryLong(item.selectFirst("a[href*='pmid=']")?.absUrl("href"), "pmid")
                ?: (index + 1).toLong()
            val text = item.selectFirst(".pm_content, .message, .message-content, .t_f")?.text()?.trim()
                ?: item.text().trim()
            if (text.isBlank()) return@mapIndexedNotNull null
            ConversationMessage(
                id = id,
                author = item.selectFirst(".author, .username, .pm_name, a[href*=space]")?.text()?.trim()
                    .orEmpty()
                    .ifBlank { "論壇會員" },
                text = text,
                createdAt = item.selectFirst(".time, .dateline, em")?.text()?.trim().orEmpty(),
                isRead = !item.classNames().any { it.contains("unread", true) },
            )
        }.distinctBy { it.id }
        if (messages.isEmpty()) {
            return ForumResult.Failure(
                com.william12233.sehuatang.data.model.ForumFailure.Parse("對話頁沒有找到可辨識的訊息節點。"),
            )
        }
        return ForumResult.Success(
            ForumConversation(
                id = ParserSupport.queryString(sourceUrl, "plid") ?: ParserSupport.queryString(sourceUrl, "pmid").orEmpty(),
                title = document.selectFirst("h1, h2, .pm_subject, .conversation-title")?.text()?.trim()
                    .orEmpty()
                    .ifBlank { "私訊對話" },
                messages = messages,
                sourceUrl = sourceUrl,
            ),
        )
    }
}

object MessageReplyFormParser {
    fun parse(html: String, sourceUrl: String): ForumResult<MessageReplyForm> {
        ForumPageClassifier.verificationMessage(html)?.let { message ->
            return ForumResult.Failure(
                com.william12233.sehuatang.data.model.ForumFailure.Verification(sourceUrl, message),
            )
        }
        val document = Jsoup.parse(html, sourceUrl)
        val form = document.select("form").firstOrNull { it.select("textarea[name]").isNotEmpty() }
            ?: return ForumResult.Failure(
                com.william12233.sehuatang.data.model.ForumFailure.Parse("對話頁沒有找到訊息輸入表單。"),
            )
        val content = form.selectFirst("textarea[name]")?.attr("name")
            ?: return ForumResult.Failure(
                com.william12233.sehuatang.data.model.ForumFailure.Parse("訊息表單沒有正文欄位名稱。"),
            )
        return ForumResult.Success(
            MessageReplyForm(
                action = form.absUrl("action").ifBlank { sourceUrl },
                method = form.attr("method").ifBlank { "POST" }.uppercase(),
                hiddenFields = form.select("input[type=hidden][name]")
                    .associate { it.attr("name") to it.attr("value") },
                contentField = content,
                sourceUrl = sourceUrl,
            ),
        )
    }
}

object ProfileParser {
    fun parse(html: String, sourceUrl: String): ForumResult<UserProfile> {
        ForumPageClassifier.verificationMessage(html)?.let { message ->
            return ForumResult.Failure(
                com.william12233.sehuatang.data.model.ForumFailure.Verification(sourceUrl, message),
            )
        }
        if (ForumPageClassifier.looksLikeLoginPage(html)) {
            return ForumResult.Failure(com.william12233.sehuatang.data.model.ForumFailure.AuthenticationRequired)
        }
        val document = Jsoup.parse(html, sourceUrl)
        val username = document.selectFirst(
            ".vwmy, .uinfo h1, .mn h1, .member-name, a[href*='uid=']",
        )?.text()?.trim().orEmpty()
        if (username.isBlank()) {
            return ForumResult.Failure(
                com.william12233.sehuatang.data.model.ForumFailure.Parse("會員頁沒有找到使用者名稱節點。"),
            )
        }
        return ForumResult.Success(
            UserProfile(
                username = username,
                avatarUrl = document.selectFirst("img.avatar, .avt img, .uinfo img")?.absUrl("src"),
                points = document.selectFirst(".credit, .integral, [class*=credit i], [class*=point i]")?.text()?.trim(),
                group = document.selectFirst(".group, .user-group, .xi2")?.text()?.trim(),
                url = sourceUrl,
            ),
        )
    }
}

private object ParserSupport {
    fun parseSections(document: org.jsoup.nodes.Document): List<ForumSection> {
        val seen = linkedSetOf<Long>()
        return document.select("a[href]").mapNotNull { link ->
            val url = link.absUrl("href")
            val id = queryLong(url, "fid") ?: return@mapNotNull null
            if (!url.contains("forumdisplay", ignoreCase = true)) return@mapNotNull null
            if (!seen.add(id)) return@mapNotNull null
            val container = link.parents().firstOrNull {
                it.tagName() in setOf("li", "tr", "dl", "div") &&
                    it.select("a[href*='forumdisplay'][href*='fid=']").size <= 4
            }
            ForumSection(
                id = id,
                name = link.text().trim().ifBlank { "版區 $id" },
                description = container?.selectFirst(".desc, .fdesc, .forum-desc, p")?.text()?.trim().orEmpty(),
                category = container?.selectFirst(".bm_h, .category, h2, h3")?.text()?.trim().orEmpty(),
                threadCount = container?.selectFirst(".num, .threads, [class*=thread i]")?.text()?.let(::parseCount),
                postCount = container?.selectFirst(".posts, [class*=post i]")?.text()?.let(::parseCount),
                iconUrl = container?.selectFirst("img")?.absUrl("src"),
                url = url,
            )
        }
    }

    fun parseThreads(document: org.jsoup.nodes.Document): List<ForumThread> {
        val seen = linkedSetOf<Long>()
        val links = document.select("a[href]").filter { link ->
            val href = link.absUrl("href")
            href.contains("viewthread", ignoreCase = true) && queryLong(href, "tid") != null
        }
        return links.mapNotNull { link ->
            val id = queryLong(link.absUrl("href"), "tid") ?: return@mapNotNull null
            if (!seen.add(id)) return@mapNotNull null
            val row = link.parents().firstOrNull {
                it.tagName() in setOf("li", "tr", "tbody", "article", "div") &&
                    it.select("a[href*='viewthread'][href*='tid=']").size <= 3
            } ?: link.parent()
            val classText = row.classNames().joinToString(" ") + " " + row.text()
            val author = row.selectFirst(
                ".by a[href*=uid], .author, .username, .thread-author, [class*=author i]",
            )?.text()?.trim().orEmpty()
            val date = row.selectFirst(
                ".by em, .by .dateline, .lastpost, .dateline, .time, [class*=time i]",
            )?.text()?.trim().orEmpty()
            val numbers = row.selectFirst(".num, .reply, .views, .stats, [class*=count i]")?.text().orEmpty()
            val numberParts = Regex("\\d[\\d,，.]*").findAll(numbers).map { parseCount(it.value) }.toList()
            ForumThread(
                id = id,
                forumId = queryLong(link.absUrl("href"), "fid"),
                title = link.text().trim().ifBlank { "未命名文章" },
                author = author,
                authorAvatar = row.selectFirst("img.avatar, .avatar img, .avt img")?.absUrl("src"),
                createdAt = date,
                lastReplyAt = row.selectFirst(".lastpost, .last-reply")?.text()?.trim().orEmpty(),
                views = numberParts.getOrNull(1),
                replies = numberParts.firstOrNull(),
                category = row.selectFirst(".tag, .label, .common, [class*=type i]")?.text()?.trim().orEmpty(),
                isPinned = classText.contains("stick", true) || classText.contains("置頂"),
                isLocked = classText.contains("lock", true) || classText.contains("關閉"),
                thumbnail = row.selectFirst("img[src], img[data-src]")?.let { image ->
                    image.absUrl("src").ifBlank { image.absUrl("data-src") }.ifBlank { null }
                },
                url = link.absUrl("href"),
            )
        }
    }

    fun parseAttachments(element: Element): List<ForumAttachment> =
        element.select("a[href]").mapNotNull { link ->
            val url = link.absUrl("href")
            val isAttachment = url.contains("attachment", true) ||
                url.contains("aid=", true) ||
                link.classNames().any { it.contains("attach", true) }
            if (!isAttachment || url.isBlank()) return@mapNotNull null
            ForumAttachment(
                id = queryString(url, "aid"),
                title = link.text().trim().ifBlank { "論壇附件" },
                url = url,
                fileName = link.attr("download").ifBlank { null },
                size = link.parent()?.text()?.let { text ->
                    Regex("\\b\\d+(?:\\.\\d+)?\\s*(?:KB|MB|GB|K|M|G)\\b", RegexOption.IGNORE_CASE)
                        .find(text)?.value
                },
                mimeType = null,
                requiresVerification = !url.contains("sehuatang.org", true),
            )
        }.distinctBy { it.url }

    fun parseContentBlocks(
        element: Element,
        sourceUrl: String,
        attachments: List<ForumAttachment>,
    ): List<ContentBlock> {
        val blocks = mutableListOf<ContentBlock>()
        element.childNodes().forEach { appendContentNode(it, blocks, sourceUrl) }
        attachments.forEach { attachment ->
            if (blocks.none { it is ContentBlock.AttachmentBlock && it.attachment.url == attachment.url }) {
                blocks += ContentBlock.AttachmentBlock(attachment)
            }
        }
        if (blocks.isEmpty() && element.text().isNotBlank()) blocks += ContentBlock.Text(element.text().trim())
        return mergeTextBlocks(blocks)
    }

    private fun appendContentNode(node: Node, blocks: MutableList<ContentBlock>, sourceUrl: String) {
        when (node) {
            is TextNode -> node.text().trim().takeIf(String::isNotBlank)?.let { blocks += ContentBlock.Text(it) }
            is Element -> when (node.tagName().lowercase()) {
                "img" -> node.absUrl("src").ifBlank { node.absUrl("data-src") }
                    .takeIf(String::isNotBlank)
                    ?.let { url ->
                        blocks += ContentBlock.Image(
                            url = url,
                            alt = node.attr("alt"),
                            width = node.attr("width").toIntOrNull(),
                            height = node.attr("height").toIntOrNull(),
                        )
                    }
                "a" -> {
                    val url = node.absUrl("href")
                    if (url.isNotBlank()) blocks += ContentBlock.Link(node.text().trim().ifBlank { url }, url)
                    else node.childNodes().forEach { appendContentNode(it, blocks, sourceUrl) }
                }
                "blockquote" -> blocks += ContentBlock.Quote(
                    author = node.selectFirst("cite, .quote-author")?.text()?.trim(),
                    blocks = parseContentBlocks(node, sourceUrl, emptyList()),
                )
                "pre", "code" -> blocks += ContentBlock.Code(
                    language = node.attr("class").takeIf { it.isNotBlank() },
                    code = node.text(),
                )
                "iframe", "video", "source" -> node.absUrl("src").ifBlank { node.absUrl("href") }
                    .takeIf(String::isNotBlank)
                    ?.let { blocks += ContentBlock.Video(it) }
                else -> node.childNodes().forEach { appendContentNode(it, blocks, sourceUrl) }
            }
        }
    }

    private fun mergeTextBlocks(blocks: List<ContentBlock>): List<ContentBlock> {
        val merged = mutableListOf<ContentBlock>()
        blocks.forEach { block ->
            if (block is ContentBlock.Text && merged.lastOrNull() is ContentBlock.Text) {
                val previous = merged.removeAt(merged.lastIndex) as ContentBlock.Text
                merged += ContentBlock.Text("${previous.value}\n${block.value}")
            } else merged += block
        }
        return merged
    }

    fun currentPage(document: org.jsoup.nodes.Document): Int =
        document.select(".pg a[href], .pagination a[href]")
            .mapNotNull { queryLong(it.absUrl("href"), "page")?.toInt() }
            .maxOrNull() ?: 1

    fun totalPages(document: org.jsoup.nodes.Document): Int =
        document.select(".pg a[href], .pagination a[href]")
            .mapNotNull { queryLong(it.absUrl("href"), "page")?.toInt() }
            .maxOrNull() ?: 1

    fun pageLink(document: org.jsoup.nodes.Document, previous: Boolean): String? {
        val candidates = document.select(".pg a[href], .pagination a[href]")
        val words = if (previous) setOf("上一頁", "上頁", "prev", "previous")
        else setOf("下一頁", "下頁", "next")
        return candidates.firstOrNull { link ->
            val text = link.text().trim().lowercase()
            words.any { text.contains(it.lowercase()) }
        }?.absUrl("href")
    }

    fun queryLong(url: String?, key: String): Long? = queryString(url, key)?.toLongOrNull()

    fun queryString(url: String?, key: String): String? {
        if (url.isNullOrBlank()) return null
        return runCatching {
            URI(url).rawQuery.orEmpty().split('&').firstOrNull { pair ->
                pair.substringBefore('=').equals(key, true)
            }?.substringAfter('=', "")
                ?.replace("+", " ")
                ?.let { java.net.URLDecoder.decode(it, Charsets.UTF_8.name()) }
        }.getOrNull()?.ifBlank { null }
    }

    fun numericId(value: String?, prefix: String): Long? =
        value?.removePrefix(prefix)?.takeIf { it != value }?.toLongOrNull()

    private fun parseCount(value: String): Int? =
        value.replace(",", "").replace("，", "").replace(".", "").toIntOrNull()
}
