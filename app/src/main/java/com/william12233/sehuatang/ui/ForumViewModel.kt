package com.william12233.sehuatang.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
import com.william12233.sehuatang.data.model.SearchResult
import com.william12233.sehuatang.data.model.UserProfile
import com.william12233.sehuatang.data.repository.ForumRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface AsyncState<out T> {
    data object Idle : AsyncState<Nothing>

    data object Loading : AsyncState<Nothing>

    data class Data<T>(val value: T) : AsyncState<T>

    data class Error(val failure: ForumFailure) : AsyncState<Nothing>
}

data class VerificationRequest(
    val url: String,
    val message: String,
)

data class SessionUiState(
    val username: String? = null,
    val isLoggedIn: Boolean = false,
)

data class ForumUiState(
    val home: AsyncState<ForumHome> = AsyncState.Idle,
    val forumList: AsyncState<ForumThreadPage> = AsyncState.Idle,
    val thread: AsyncState<ForumThreadPage> = AsyncState.Idle,
    val search: AsyncState<SearchResult> = AsyncState.Idle,
    val messages: AsyncState<List<ForumMessage>> = AsyncState.Idle,
    val conversation: AsyncState<ForumConversation> = AsyncState.Idle,
    val messageReplyForm: AsyncState<MessageReplyForm> = AsyncState.Idle,
    val messageSubmit: AsyncState<Unit> = AsyncState.Idle,
    val profile: AsyncState<UserProfile> = AsyncState.Idle,
    val loginForm: AsyncState<LoginForm> = AsyncState.Idle,
    val replyForm: AsyncState<PostForm> = AsyncState.Idle,
    val submit: AsyncState<Unit> = AsyncState.Idle,
    val session: SessionUiState = SessionUiState(),
    val verification: VerificationRequest? = null,
    val selectedAttachmentNames: List<String> = emptyList(),
)

class ForumViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ForumRepository(application)
    private val _state = MutableStateFlow(ForumUiState())
    val state = _state.asStateFlow()

    private var pendingVerificationRetry: (() -> Unit)? = null
    private var selectedAttachmentUris: List<Uri> = emptyList()

    init {
        loadHome()
    }

    fun loadHome() {
        viewModelScope.launch {
            _state.update { it.copy(home = AsyncState.Loading) }
            when (val result = repository.loadHome()) {
                is ForumResult.Success -> _state.update { it.copy(home = AsyncState.Data(result.value)) }
                is ForumResult.Failure -> {
                    _state.update { it.copy(home = AsyncState.Error(result.error)) }
                    captureVerification(result.error) { loadHome() }
                }
            }
        }
    }

    fun loadForum(forumId: Long, page: Int = 1) {
        viewModelScope.launch {
            _state.update { it.copy(forumList = AsyncState.Loading) }
            when (val result = repository.loadForumList(forumId, page)) {
                is ForumResult.Success -> _state.update { it.copy(forumList = AsyncState.Data(result.value)) }
                is ForumResult.Failure -> {
                    _state.update { it.copy(forumList = AsyncState.Error(result.error)) }
                    captureVerification(result.error) { loadForum(forumId, page) }
                }
            }
        }
    }

    fun loadThread(url: String) {
        viewModelScope.launch {
            _state.update { it.copy(thread = AsyncState.Loading) }
            when (val result = repository.loadThread(url)) {
                is ForumResult.Success -> _state.update { it.copy(thread = AsyncState.Data(result.value)) }
                is ForumResult.Failure -> {
                    _state.update { it.copy(thread = AsyncState.Error(result.error)) }
                    captureVerification(result.error) { loadThread(url) }
                }
            }
        }
    }

    fun search(query: String) {
        viewModelScope.launch {
            _state.update { it.copy(search = AsyncState.Loading) }
            when (val result = repository.search(query)) {
                is ForumResult.Success -> _state.update { it.copy(search = AsyncState.Data(result.value)) }
                is ForumResult.Failure -> {
                    _state.update { it.copy(search = AsyncState.Error(result.error)) }
                    captureVerification(result.error) { search(query) }
                }
            }
        }
    }

    fun loadMessages() {
        viewModelScope.launch {
            _state.update { it.copy(messages = AsyncState.Loading) }
            when (val result = repository.loadMessages()) {
                is ForumResult.Success -> _state.update { it.copy(messages = AsyncState.Data(result.value)) }
                is ForumResult.Failure -> {
                    _state.update { it.copy(messages = AsyncState.Error(result.error)) }
                    captureVerification(result.error) { loadMessages() }
                }
            }
        }
    }

    fun loadConversation(url: String) {
        viewModelScope.launch {
            _state.update { it.copy(conversation = AsyncState.Loading, messageReplyForm = AsyncState.Idle) }
            when (val result = repository.loadConversation(url)) {
                is ForumResult.Success -> {
                    _state.update { it.copy(conversation = AsyncState.Data(result.value)) }
                    loadMessageReplyForm(url)
                }
                is ForumResult.Failure -> {
                    _state.update { it.copy(conversation = AsyncState.Error(result.error)) }
                    captureVerification(result.error) { loadConversation(url) }
                }
            }
        }
    }

    private fun loadMessageReplyForm(url: String) {
        viewModelScope.launch {
            _state.update { it.copy(messageReplyForm = AsyncState.Loading) }
            when (val result = repository.loadMessageReplyForm(url)) {
                is ForumResult.Success -> _state.update { it.copy(messageReplyForm = AsyncState.Data(result.value)) }
                is ForumResult.Failure -> {
                    _state.update { it.copy(messageReplyForm = AsyncState.Error(result.error)) }
                    captureVerification(result.error) { loadMessageReplyForm(url) }
                }
            }
        }
    }

    fun submitMessage(content: String) {
        val form = (_state.value.messageReplyForm as? AsyncState.Data)?.value ?: return
        viewModelScope.launch {
            _state.update { it.copy(messageSubmit = AsyncState.Loading) }
            when (val result = repository.submitMessageReply(form, content)) {
                is ForumResult.Success -> {
                    _state.update { it.copy(messageSubmit = AsyncState.Data(Unit)) }
                    loadConversation(form.sourceUrl)
                }
                is ForumResult.Failure -> {
                    _state.update { it.copy(messageSubmit = AsyncState.Error(result.error)) }
                    captureVerification(result.error) { submitMessage(content) }
                }
            }
        }
    }

    fun loadLoginForm() {
        viewModelScope.launch {
            _state.update { it.copy(loginForm = AsyncState.Loading) }
            when (val result = repository.loadLoginForm()) {
                is ForumResult.Success -> _state.update { it.copy(loginForm = AsyncState.Data(result.value)) }
                is ForumResult.Failure -> {
                    _state.update { it.copy(loginForm = AsyncState.Error(result.error)) }
                    captureVerification(result.error) { loadLoginForm() }
                }
            }
        }
    }

    fun login(credentials: LoginCredentials) {
        viewModelScope.launch {
            _state.update { it.copy(loginForm = AsyncState.Loading) }
            when (val result = repository.login(credentials)) {
                is ForumResult.Success -> _state.update {
                    it.copy(
                        loginForm = AsyncState.Idle,
                        session = SessionUiState(credentials.username, isLoggedIn = true),
                    )
                }.also { loadProfile() }

                is ForumResult.Failure -> {
                    _state.update { it.copy(loginForm = AsyncState.Error(result.error)) }
                    captureVerification(result.error) { login(credentials) }
                }
            }
        }
    }

    fun loadReplyForm(sourceUrl: String) {
        viewModelScope.launch {
            _state.update { it.copy(replyForm = AsyncState.Loading) }
            when (val result = repository.loadReplyForm(sourceUrl)) {
                is ForumResult.Success -> _state.update { it.copy(replyForm = AsyncState.Data(result.value)) }
                is ForumResult.Failure -> {
                    _state.update { it.copy(replyForm = AsyncState.Error(result.error)) }
                    captureVerification(result.error) { loadReplyForm(sourceUrl) }
                }
            }
        }
    }

    fun loadProfile() {
        viewModelScope.launch {
            _state.update { it.copy(profile = AsyncState.Loading) }
            when (val result = repository.loadProfile()) {
                is ForumResult.Success -> _state.update { it.copy(profile = AsyncState.Data(result.value)) }
                is ForumResult.Failure -> {
                    _state.update { it.copy(profile = AsyncState.Error(result.error)) }
                    captureVerification(result.error) { loadProfile() }
                }
            }
        }
    }

    fun submitPost(title: String?, content: String, quote: String? = null) {
        val form = (_state.value.replyForm as? AsyncState.Data)?.value ?: return
        viewModelScope.launch {
            _state.update { it.copy(submit = AsyncState.Loading) }
            when (val result = repository.submitPost(form, title, content, quote, selectedAttachmentUris)) {
                is ForumResult.Success -> {
                    selectedAttachmentUris = emptyList()
                    _state.update { it.copy(submit = AsyncState.Data(Unit), selectedAttachmentNames = emptyList()) }
                }
                is ForumResult.Failure -> {
                    _state.update { it.copy(submit = AsyncState.Error(result.error)) }
                    captureVerification(result.error) { submitPost(title, content, quote) }
                }
            }
        }
    }

    fun onVerificationCompleted(url: String, cookieHeader: String?) {
        importVerificationCookies(url, cookieHeader)
        val retry = pendingVerificationRetry
        pendingVerificationRetry = null
        _state.update { it.copy(verification = null) }
        retry?.invoke()
    }

    fun dismissVerification() {
        pendingVerificationRetry = null
        _state.update { it.copy(verification = null) }
    }

    fun importVerificationCookies(url: String, cookieHeader: String?) {
        repository.importVerificationCookies(url, cookieHeader)
    }

    fun clearSession() {
        repository.clearSession()
        _state.update {
            it.copy(
                session = SessionUiState(),
                loginForm = AsyncState.Idle,
                profile = AsyncState.Idle,
                conversation = AsyncState.Idle,
                messageReplyForm = AsyncState.Idle,
            )
        }
    }

    fun cookieHeader(url: String): String? = repository.cookieHeader(url)

    fun setSelectedAttachments(uris: List<Uri>) {
        selectedAttachmentUris = uris
        _state.update {
            it.copy(
                selectedAttachmentNames = uris.map { uri -> uri.lastPathSegment?.substringAfterLast('/') ?: "附件" },
            )
        }
    }

    private fun captureVerification(error: ForumFailure, retry: () -> Unit) {
        if (error is ForumFailure.Verification) {
            pendingVerificationRetry = retry
            _state.update {
                it.copy(verification = VerificationRequest(error.url, error.message))
            }
        }
    }
}
