package com.william12233.sehuatang.data.model

/**
 * 論壇資料模型。這些模型是 App UI 的唯一資料來源；Compose 不接收 HTML。
 */
data class ForumHome(
    val sections: List<ForumSection>,
    val latestThreads: List<ForumThread>,
    val popularThreads: List<ForumThread>,
    val sourceUrl: String,
)

data class ForumSection(
    val id: Long,
    val name: String,
    val description: String = "",
    val category: String = "",
    val threadCount: Int? = null,
    val postCount: Int? = null,
    val iconUrl: String? = null,
    val url: String,
)

data class ForumThread(
    val id: Long,
    val forumId: Long? = null,
    val title: String,
    val author: String = "",
    val authorAvatar: String? = null,
    val createdAt: String = "",
    val lastReplyAt: String = "",
    val views: Int? = null,
    val replies: Int? = null,
    val category: String = "",
    val isPinned: Boolean = false,
    val isLocked: Boolean = false,
    val thumbnail: String? = null,
    val url: String,
)

data class ForumThreadPage(
    val thread: ForumThread,
    val posts: List<ForumPost>,
    val threads: List<ForumThread> = listOf(thread),
    val page: Int = 1,
    val totalPages: Int = 1,
    val previousPageUrl: String? = null,
    val nextPageUrl: String? = null,
    val sourceUrl: String,
)

data class ForumPost(
    val id: Long,
    val threadId: Long,
    val author: String,
    val authorAvatar: String? = null,
    val floor: String = "",
    val createdAt: String = "",
    val contentBlocks: List<ContentBlock>,
    val attachments: List<ForumAttachment> = emptyList(),
    val quotedPost: QuotedPost? = null,
    val editInformation: String? = null,
)

data class QuotedPost(
    val author: String? = null,
    val text: String,
    val postId: Long? = null,
)

sealed interface ContentBlock {
    data class Text(val value: String) : ContentBlock

    data class Image(
        val url: String,
        val alt: String = "",
        val width: Int? = null,
        val height: Int? = null,
    ) : ContentBlock

    data class Link(val label: String, val url: String) : ContentBlock

    data class Quote(
        val author: String? = null,
        val blocks: List<ContentBlock>,
    ) : ContentBlock

    data class Code(val language: String? = null, val code: String) : ContentBlock

    data class AttachmentBlock(val attachment: ForumAttachment) : ContentBlock

    data class Video(val url: String) : ContentBlock

    data class Unsupported(val reason: String, val text: String = "") : ContentBlock
}

data class ForumAttachment(
    val id: String? = null,
    val title: String,
    val url: String,
    val fileName: String? = null,
    val size: String? = null,
    val mimeType: String? = null,
    val requiresVerification: Boolean = false,
)

data class SearchResult(
    val threads: List<ForumThread>,
    val page: Int = 1,
    val totalPages: Int = 1,
    val nextPageUrl: String? = null,
    val sourceUrl: String,
)

data class SearchForm(
    val action: String,
    val method: String,
    val hiddenFields: Map<String, String>,
    val queryField: String,
    val sourceUrl: String,
)

data class LoginCredentials(
    val username: String,
    val password: String,
    val securityAnswer: String? = null,
    val rememberMe: Boolean = true,
)

data class ForumMessage(
    val id: Long,
    val conversationId: String,
    val author: String,
    val authorAvatar: String? = null,
    val preview: String,
    val createdAt: String,
    val isRead: Boolean,
    val url: String,
)

data class ForumConversation(
    val id: String,
    val title: String,
    val messages: List<ConversationMessage>,
    val sourceUrl: String,
)

data class ConversationMessage(
    val id: Long,
    val author: String,
    val text: String,
    val createdAt: String,
    val isRead: Boolean = true,
)

data class MessageReplyForm(
    val action: String,
    val method: String,
    val hiddenFields: Map<String, String>,
    val contentField: String,
    val sourceUrl: String,
)

data class UserProfile(
    val username: String,
    val avatarUrl: String? = null,
    val points: String? = null,
    val group: String? = null,
    val url: String,
)

data class LoginForm(
    val action: String,
    val hiddenFields: Map<String, String>,
    val usernameField: String,
    val passwordField: String,
    val securityQuestionField: String? = null,
    val rememberField: String? = null,
    val rememberValue: String? = null,
    val captchaDescription: String? = null,
    val sourceUrl: String,
)

data class PostForm(
    val action: String,
    val method: String,
    val hiddenFields: Map<String, String>,
    val titleField: String? = null,
    val contentField: String,
    val quoteField: String? = null,
    val uploadField: String? = null,
    val sourceUrl: String,
    val requiresCaptcha: Boolean = false,
)

sealed interface ForumFailure {
    data class Network(val message: String, val cause: Throwable? = null) : ForumFailure

    data class Http(val code: Int, val message: String) : ForumFailure

    data class Parse(val message: String) : ForumFailure

    data class Verification(
        val url: String,
        val message: String,
    ) : ForumFailure

    data object AuthenticationRequired : ForumFailure

    data class PermissionDenied(val message: String) : ForumFailure
}

sealed interface ForumResult<out T> {
    data class Success<T>(val value: T) : ForumResult<T>

    data class Failure(val error: ForumFailure) : ForumResult<Nothing>
}
