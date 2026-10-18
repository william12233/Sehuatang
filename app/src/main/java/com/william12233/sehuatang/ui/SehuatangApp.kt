@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.william12233.sehuatang.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.william12233.sehuatang.data.model.ForumAttachment
import com.william12233.sehuatang.ui.composer.ComposerScreen
import com.william12233.sehuatang.ui.forum.ForumListScreen
import com.william12233.sehuatang.ui.forum.ForumSectionsScreen
import com.william12233.sehuatang.ui.home.HomeScreen
import com.william12233.sehuatang.ui.login.LoginScreen
import com.william12233.sehuatang.ui.messages.MessagesScreen
import com.william12233.sehuatang.ui.messages.MessageConversationScreen
import com.william12233.sehuatang.ui.profile.ProfileScreen
import com.william12233.sehuatang.ui.search.SearchScreen
import com.william12233.sehuatang.ui.thread.ThreadDetailScreen
import com.william12233.sehuatang.ui.verification.VerificationScreen
import com.william12233.sehuatang.web.download.DownloadRequest
import com.william12233.sehuatang.web.download.DownloadProgress
import com.william12233.sehuatang.web.download.DownloadStatus
import com.william12233.sehuatang.web.download.DownloadViewModel

private enum class AppTab(val route: String, val label: String) {
    HOME("home", "首頁"),
    FORUM("forum", "版區"),
    SEARCH("search", "搜尋"),
    MESSAGES("messages", "訊息"),
    PROFILE("profile", "我的"),
}

private val mainRoutes = AppTab.entries.map { it.route }.toSet()

@Composable
fun SehuaTangApp(
    appViewModel: AppViewModel,
    forumViewModel: ForumViewModel,
    downloadViewModel: DownloadViewModel,
    onOpenDownloads: () -> Unit,
    onDownloadAttachment: (DownloadRequest) -> Unit,
    onOpenExternal: (String) -> Unit,
    onShareLink: (String) -> Unit,
    onPickAttachments: () -> Unit,
) {
    val appState by appViewModel.recentVisits.collectAsStateWithLifecycle()
    val bookmarks by appViewModel.bookmarks.collectAsStateWithLifecycle()
    val themeMode by appViewModel.themeMode.collectAsStateWithLifecycle()
    val forumState by forumViewModel.state.collectAsStateWithLifecycle()
    val downloadProgress by downloadViewModel.current.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var route by rememberSaveable { mutableStateOf(AppTab.HOME.route) }
    var previousRoute by rememberSaveable { mutableStateOf(AppTab.HOME.route) }
    var threadUrl by rememberSaveable { mutableStateOf("") }
    var conversationUrl by rememberSaveable { mutableStateOf("") }
    var forumId by rememberSaveable { mutableStateOf(0L) }
    var verificationReturnRoute by rememberSaveable { mutableStateOf(AppTab.HOME.route) }
    var attachmentVerificationUrl by rememberSaveable { mutableStateOf("") }
    var attachmentVerificationMessage by rememberSaveable { mutableStateOf("") }
    var pendingAttachment by remember { mutableStateOf<ForumAttachment?>(null) }

    val selectedTab = AppTab.entries.firstOrNull { it.route == route } ?: AppTab.HOME
    val isMainRoute = route in mainRoutes

    fun goToTab(tab: AppTab) {
        route = tab.route
        if (tab == AppTab.MESSAGES) forumViewModel.loadMessages()
    }

    fun openThread(url: String, title: String? = null) {
        previousRoute = route
        threadUrl = url
        route = "thread"
        title?.let { appViewModel.recordVisit(it, url) }
        forumViewModel.loadThread(url)
    }

    fun openVerification() {
        if (forumState.verification != null) {
            verificationReturnRoute = route
            route = "verification"
        }
    }

    fun enqueueAttachment(attachment: ForumAttachment) {
        onDownloadAttachment(
            DownloadRequest(
                url = attachment.url,
                userAgent = "Mozilla/5.0 (Linux; Android 14) SehuatangAndroid/1.0",
                contentDisposition = attachment.fileName,
                mimeType = attachment.mimeType,
                referer = threadUrl,
                cookie = forumViewModel.cookieHeader(attachment.url),
            ),
        )
    }

    fun goBack() {
        route = previousRoute.takeIf { it in mainRoutes || it == "thread" || it == "forum-list" } ?: AppTab.HOME.route
    }

    BackHandler(enabled = route != "verification" && route != "attachment-verification" && !isMainRoute) { goBack() }

    LaunchedEffect(forumState.submit) {
        if (forumState.submit is AsyncState.Data) snackbarHostState.showSnackbar("伺服器已確認送出結果")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (route) {
                            "thread" -> "文章"
                            "forum-list" -> "文章列表"
                            "login" -> "登入論壇"
                            "composer" -> "原生編輯器"
                            "conversation" -> "私訊對話"
                            "verification" -> "站點驗證"
                            else -> selectedTab.label
                        },
                    )
                },
                navigationIcon = if (isMainRoute) {
                    {}
                } else if (route == "verification") {
                    {}
                } else {
                    { IconButton(onClick = ::goBack) { Text("返回") } }
                },
                actions = {
                    if (route == AppTab.HOME.route) {
                        IconButton(onClick = forumViewModel::loadHome) {
                            Icon(Icons.Outlined.Refresh, contentDescription = "重新整理")
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (isMainRoute) {
                Column {
                    downloadProgress?.let { progress ->
                        DownloadStatusBar(
                            progress = progress,
                            onOpen = { downloadViewModel.openCurrent() },
                            onCancel = { downloadViewModel.cancelCurrent() },
                            onDismiss = { downloadViewModel.dismiss() },
                        )
                    }
                    NavigationBar {
                        NavigationBarItem(
                        selected = selectedTab == AppTab.HOME,
                        onClick = { goToTab(AppTab.HOME) },
                        icon = { Icon(Icons.Outlined.Home, contentDescription = null) },
                        label = { Text("首頁") },
                        )
                        NavigationBarItem(
                        selected = selectedTab == AppTab.FORUM,
                        onClick = { goToTab(AppTab.FORUM) },
                        icon = { Icon(Icons.Outlined.Forum, contentDescription = null) },
                        label = { Text("版區") },
                        )
                        NavigationBarItem(
                        selected = selectedTab == AppTab.SEARCH,
                        onClick = { goToTab(AppTab.SEARCH) },
                        icon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        label = { Text("搜尋") },
                        )
                        NavigationBarItem(
                        selected = selectedTab == AppTab.MESSAGES,
                        onClick = { goToTab(AppTab.MESSAGES) },
                        icon = { Icon(Icons.Outlined.MailOutline, contentDescription = null) },
                        label = { Text("訊息") },
                        )
                        NavigationBarItem(
                        selected = selectedTab == AppTab.PROFILE,
                        onClick = { goToTab(AppTab.PROFILE) },
                        icon = { Icon(Icons.Outlined.PersonOutline, contentDescription = null) },
                        label = { Text("我的") },
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            when (route) {
                AppTab.HOME.route -> HomeScreen(
                    home = forumState.home,
                    recentVisits = appState,
                    onRefresh = forumViewModel::loadHome,
                    onOpenSection = { section ->
                        previousRoute = AppTab.HOME.route
                        forumId = section.id
                        route = "forum-list"
                        forumViewModel.loadForum(section.id)
                    },
                    onOpenThread = { openThread(it.url, it.title) },
                    onOpenRecent = { openThread(it) },
                    onOpenLogin = {
                        previousRoute = AppTab.HOME.route
                        route = "login"
                        forumViewModel.loadLoginForm()
                    },
                    onOpenVerification = ::openVerification,
                )

                AppTab.FORUM.route -> ForumSectionsScreen(
                    state = forumState.home,
                    onRetry = forumViewModel::loadHome,
                    onOpenSection = { section ->
                        previousRoute = AppTab.FORUM.route
                        forumId = section.id
                        route = "forum-list"
                        forumViewModel.loadForum(section.id)
                    },
                    onVerification = ::openVerification,
                )

                AppTab.SEARCH.route -> SearchScreen(
                    state = forumState.search,
                    onSearch = forumViewModel::search,
                    onOpenThread = { openThread(it) },
                    onVerification = ::openVerification,
                )

                AppTab.MESSAGES.route -> MessagesScreen(
                    state = forumState.messages,
                    onRetry = forumViewModel::loadMessages,
                    onLogin = {
                        previousRoute = AppTab.MESSAGES.route
                        route = "login"
                        forumViewModel.loadLoginForm()
                    },
                    onVerification = ::openVerification,
                    onOpenMessage = { message ->
                        previousRoute = AppTab.MESSAGES.route
                        conversationUrl = message.url
                        route = "conversation"
                        forumViewModel.loadConversation(message.url)
                    },
                )

                AppTab.PROFILE.route -> ProfileScreen(
                    bookmarks = bookmarks,
                    recentVisits = appState,
                    themeMode = themeMode,
                    session = forumState.session,
                    profile = forumState.profile,
                    onThemeModeChanged = appViewModel::setThemeMode,
                    onOpenLogin = {
                        previousRoute = AppTab.PROFILE.route
                        route = "login"
                        forumViewModel.loadLoginForm()
                    },
                    onOpenThread = { openThread(it) },
                    onOpenDownloads = onOpenDownloads,
                    onRemoveBookmark = appViewModel::removeBookmark,
                    onClearHistory = appViewModel::clearRecentVisits,
                    onClearSession = forumViewModel::clearSession,
                )

                "forum-list" -> ForumListScreen(
                    state = forumState.forumList,
                    onRetry = { forumViewModel.loadForum(forumId) },
                    onOpenThread = { openThread(it) },
                    onNewThread = { sourceUrl ->
                        previousRoute = "forum-list"
                        threadUrl = sourceUrl
                        route = "composer"
                        forumViewModel.loadReplyForm(sourceUrl)
                    },
                    onPageChange = { page -> forumViewModel.loadForum(forumId, page) },
                    onVerification = ::openVerification,
                )

                "thread" -> ThreadDetailScreen(
                    state = forumState.thread,
                    onRetry = { forumViewModel.loadThread(threadUrl) },
                    onReply = { url ->
                        previousRoute = "thread"
                        route = "composer"
                        forumViewModel.loadReplyForm(url)
                    },
                    onVerification = ::openVerification,
                    onOpenLink = onOpenExternal,
                    onOpenPage = forumViewModel::loadThread,
                    onShareLink = onShareLink,
                    onDownload = { attachment ->
                        if (attachment.requiresVerification) {
                            pendingAttachment = attachment
                            attachmentVerificationUrl = attachment.url
                            attachmentVerificationMessage = "這個附件位於第三方網域；請親自完成驗證，完成後再交給 Android 下載管理。"
                            verificationReturnRoute = route
                            route = "attachment-verification"
                        } else enqueueAttachment(attachment)
                    },
                    isBookmarked = appViewModel.isBookmarked(threadUrl),
                    onToggleBookmark = {
                        val page = forumState.thread as? AsyncState.Data
                        page?.value?.thread?.let { thread -> appViewModel.toggleBookmark(thread.title, thread.url) }
                    },
                    onDownloadImage = { url ->
                        onDownloadAttachment(
                            DownloadRequest(
                                url = url,
                                userAgent = "Mozilla/5.0 (Linux; Android 14) SehuatangAndroid/1.0",
                                contentDisposition = null,
                                mimeType = "image/*",
                                referer = threadUrl,
                                cookie = forumViewModel.cookieHeader(url),
                            ),
                        )
                    },
                )

                "login" -> LoginScreen(
                    state = forumState.loginForm,
                    onLoad = forumViewModel::loadLoginForm,
                    onLogin = forumViewModel::login,
                    onVerification = ::openVerification,
                    onBack = ::goBack,
                )

                "composer" -> ComposerScreen(
                    formState = forumState.replyForm,
                    submitState = forumState.submit,
                    onLoadForm = {
                        val url = (forumState.thread as? AsyncState.Data)?.value?.sourceUrl ?: threadUrl
                        forumViewModel.loadReplyForm(url)
                    },
                    onSubmit = { title, content -> forumViewModel.submitPost(title, content) },
                    onVerification = ::openVerification,
                    onPickAttachments = onPickAttachments,
                    selectedAttachmentLabel = forumState.selectedAttachmentNames.joinToString(", "),
                    onBack = ::goBack,
                )

                "conversation" -> MessageConversationScreen(
                    conversationState = forumState.conversation,
                    replyFormState = forumState.messageReplyForm,
                    submitState = forumState.messageSubmit,
                    onRetry = { forumViewModel.loadConversation(conversationUrl) },
                    onSubmit = forumViewModel::submitMessage,
                    onVerification = ::openVerification,
                )

                "verification" -> forumState.verification?.let { request ->
                    VerificationScreen(
                        url = request.url,
                        message = request.message,
                        onComplete = { cookie ->
                            forumViewModel.onVerificationCompleted(request.url, cookie)
                            route = verificationReturnRoute
                        },
                        onBack = {
                            forumViewModel.dismissVerification()
                            route = verificationReturnRoute
                        },
                    )
                }

                "attachment-verification" -> {
                    if (attachmentVerificationUrl.isNotBlank()) {
                        VerificationScreen(
                            url = attachmentVerificationUrl,
                            message = attachmentVerificationMessage,
                            onComplete = { cookie ->
                                forumViewModel.importVerificationCookies(attachmentVerificationUrl, cookie)
                                pendingAttachment?.let(::enqueueAttachment)
                                pendingAttachment = null
                                attachmentVerificationUrl = ""
                                route = verificationReturnRoute
                            },
                            onBack = {
                                pendingAttachment = null
                                attachmentVerificationUrl = ""
                                route = verificationReturnRoute
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadStatusBar(
    progress: DownloadProgress,
    onOpen: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                when (progress.status) {
                    DownloadStatus.SUCCESSFUL -> "下載完成：${progress.fileName}"
                    DownloadStatus.FAILED -> "下載失敗：${progress.fileName}"
                    else -> "下載中：${progress.fileName}"
                },
                style = MaterialTheme.typography.labelMedium,
            )
            if (progress.status == DownloadStatus.RUNNING || progress.status == DownloadStatus.PENDING) {
                LinearProgressIndicator(
                    progress = { (progress.percent ?: 0) / 100f },
                    modifier = Modifier.padding(top = 5.dp),
                )
            }
            Row {
                if (progress.status == DownloadStatus.SUCCESSFUL) TextButton(onClick = onOpen) { Text("開啟") }
                if (progress.status == DownloadStatus.RUNNING || progress.status == DownloadStatus.PENDING) {
                    TextButton(onClick = onCancel) { Text("取消") }
                }
                TextButton(onClick = onDismiss) { Text("關閉") }
            }
        }
    }
}
