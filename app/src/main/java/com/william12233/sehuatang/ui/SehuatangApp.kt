@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.william12233.sehuatang.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.william12233.sehuatang.ui.components.WebPageScreen
import com.william12233.sehuatang.ui.home.HomeScreen
import com.william12233.sehuatang.ui.profile.ProfileScreen
import com.william12233.sehuatang.ui.search.SearchScreen
import com.william12233.sehuatang.web.download.DownloadProgress
import com.william12233.sehuatang.web.download.DownloadStatus
import com.william12233.sehuatang.web.download.DownloadViewModel
import com.william12233.sehuatang.web.navigation.ForumUrls
import com.william12233.sehuatang.web.navigation.SafeUrlPolicy
import com.william12233.sehuatang.web.session.PopupWindowState
import com.william12233.sehuatang.web.session.WebEvent
import com.william12233.sehuatang.web.session.WebSessionViewModel
import com.william12233.sehuatang.web.session.WebTab

private enum class AppTab {
    HOME,
    FORUM,
    SEARCH,
    MESSAGES,
    PROFILE,
}

@Composable
fun SehuaTangApp(
    appViewModel: AppViewModel,
    webSessionViewModel: WebSessionViewModel,
    downloadViewModel: DownloadViewModel,
    onOpenDownloads: () -> Unit,
) {
    var selectedTabName by rememberSaveable { mutableStateOf(AppTab.HOME.name) }
    var searchWebMode by rememberSaveable { mutableStateOf(false) }
    var profileWebMode by rememberSaveable { mutableStateOf(false) }
    val selectedTab = runCatching { AppTab.valueOf(selectedTabName) }.getOrDefault(AppTab.HOME)
    val recentVisits by appViewModel.recentVisits.collectAsStateWithLifecycle()
    val bookmarks by appViewModel.bookmarks.collectAsStateWithLifecycle()
    val themeMode by appViewModel.themeMode.collectAsStateWithLifecycle()
    val downloadProgress by downloadViewModel.current.collectAsStateWithLifecycle()
    val popup by webSessionViewModel.popup.collectAsStateWithLifecycle()
    val snackbarHostState = androidx.compose.runtime.remember { SnackbarHostState() }

    LaunchedEffect(webSessionViewModel) {
        webSessionViewModel.events.collect { event ->
            when (event) {
                is WebEvent.Message -> snackbarHostState.showSnackbar(event.text)
            }
        }
    }

    fun openWeb(tab: WebTab, url: String) {
        when (tab) {
            WebTab.SEARCH -> searchWebMode = true
            WebTab.PROFILE -> profileWebMode = true
            else -> Unit
        }
        selectedTabName = when (tab) {
            WebTab.FORUM -> AppTab.FORUM.name
            WebTab.SEARCH -> AppTab.SEARCH.name
            WebTab.MESSAGES -> AppTab.MESSAGES.name
            WebTab.PROFILE -> AppTab.PROFILE.name
        }
        webSessionViewModel.open(tab, url)
    }

    fun selectWebTab(tab: WebTab) {
        selectedTabName = when (tab) {
            WebTab.FORUM -> AppTab.FORUM.name
            WebTab.SEARCH -> AppTab.SEARCH.name
            WebTab.MESSAGES -> AppTab.MESSAGES.name
            WebTab.PROFILE -> AppTab.PROFILE.name
        }
    }

    fun showNativeTab(tab: AppTab) {
        selectedTabName = tab.name
        when (tab) {
            AppTab.SEARCH -> searchWebMode = false
            AppTab.PROFILE -> profileWebMode = false
            else -> Unit
        }
    }

    BackHandler(
        enabled = popup == null &&
            ((selectedTab == AppTab.SEARCH && !searchWebMode) ||
                (selectedTab == AppTab.PROFILE && !profileWebMode)),
    ) {
        when (selectedTab) {
            AppTab.SEARCH, AppTab.PROFILE -> showNativeTab(AppTab.HOME)
            else -> Unit
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column {
                downloadProgress?.let {
                    DownloadStatusBar(
                        progress = it,
                        onOpen = { downloadViewModel.openCurrent() },
                        onCancel = { downloadViewModel.cancelCurrent() },
                        onDismiss = { downloadViewModel.dismiss() },
                    )
                }
                NavigationBar {
                    NavigationItem(
                        selected = selectedTab == AppTab.HOME,
                        icon = Icons.Outlined.Home,
                        label = "首頁",
                        onClick = { showNativeTab(AppTab.HOME) },
                    )
                    NavigationItem(
                        selected = selectedTab == AppTab.FORUM,
                        icon = Icons.Outlined.Forum,
                        label = "版區",
                        onClick = { selectWebTab(WebTab.FORUM) },
                    )
                    NavigationItem(
                        selected = selectedTab == AppTab.SEARCH,
                        icon = Icons.Outlined.Search,
                        label = "搜尋",
                        onClick = { showNativeTab(AppTab.SEARCH) },
                    )
                    NavigationItem(
                        selected = selectedTab == AppTab.MESSAGES,
                        icon = Icons.Outlined.MailOutline,
                        label = "訊息",
                        onClick = { selectWebTab(WebTab.MESSAGES) },
                    )
                    NavigationItem(
                        selected = selectedTab == AppTab.PROFILE,
                        icon = Icons.Outlined.PersonOutline,
                        label = "我的",
                        onClick = { showNativeTab(AppTab.PROFILE) },
                    )
                }
            }
        },
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                AppTab.HOME -> {
                    NativeTopBar(title = "首頁")
                    HomeScreen(
                        recentVisits = recentVisits,
                        onOpenWeb = ::openWeb,
                    )
                }

                AppTab.FORUM -> {
                    WebPageScreen(
                        tab = WebTab.FORUM,
                        initialUrl = ForumUrls.HOME,
                        appViewModel = appViewModel,
                        webSessionViewModel = webSessionViewModel,
                        onBackAtRoot = { showNativeTab(AppTab.HOME) },
                    )
                }

                AppTab.SEARCH -> {
                    if (searchWebMode) {
                        WebPageScreen(
                            tab = WebTab.SEARCH,
                            initialUrl = ForumUrls.SEARCH,
                            appViewModel = appViewModel,
                            webSessionViewModel = webSessionViewModel,
                            onBackAtRoot = { searchWebMode = false },
                        )
                    } else {
                        NativeTopBar(title = "搜尋")
                        SearchScreen(onOpenWeb = ::openWeb)
                    }
                }

                AppTab.MESSAGES -> {
                    WebPageScreen(
                        tab = WebTab.MESSAGES,
                        initialUrl = ForumUrls.MESSAGES,
                        appViewModel = appViewModel,
                        webSessionViewModel = webSessionViewModel,
                        onBackAtRoot = { showNativeTab(AppTab.HOME) },
                    )
                }

                AppTab.PROFILE -> {
                    if (profileWebMode) {
                        WebPageScreen(
                            tab = WebTab.PROFILE,
                            initialUrl = ForumUrls.MEMBER_HOME,
                            appViewModel = appViewModel,
                            webSessionViewModel = webSessionViewModel,
                            onBackAtRoot = { profileWebMode = false },
                        )
                    } else {
                        NativeTopBar(title = "我的")
                        ProfileScreen(
                            bookmarks = bookmarks,
                            recentVisits = recentVisits,
                            themeMode = themeMode,
                            onThemeModeChanged = appViewModel::setThemeMode,
                            onOpenWeb = ::openWeb,
                            onOpenDownloads = onOpenDownloads,
                            onRemoveBookmark = appViewModel::removeBookmark,
                            onClearHistory = appViewModel::clearRecentVisits,
                            onClearWebsiteSession = { webSessionViewModel.clearWebsiteSession() },
                        )
                    }
                }
            }
        }
    }

    popup?.let { popupState ->
        PopupWebViewDialog(
            popupState = popupState,
            webSessionViewModel = webSessionViewModel,
        )
    }
}

@Composable
private fun NativeTopBar(title: String) {
    TopAppBar(title = { Text(title) })
}

@Composable
private fun RowScope.NavigationItem(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    TextButton(
        modifier = Modifier.weight(1f),
        onClick = onClick,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
    Surface(tonalElevation = 2.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = when (progress.status) {
                        DownloadStatus.SUCCESSFUL -> "下載完成：${progress.fileName}"
                        DownloadStatus.FAILED -> "下載失敗：網站可能回傳了驗證頁"
                        else -> "下載中：${progress.fileName}"
                    },
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium,
                )
                if (progress.status == DownloadStatus.SUCCESSFUL) {
                    TextButton(onClick = onOpen) { Text("開啟") }
                } else if (
                    progress.status == DownloadStatus.PENDING ||
                    progress.status == DownloadStatus.RUNNING ||
                    progress.status == DownloadStatus.PAUSED
                ) {
                    TextButton(onClick = onCancel) { Text("取消") }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "關閉下載提示")
                }
            }
            if (progress.status == DownloadStatus.PENDING || progress.status == DownloadStatus.RUNNING) {
                LinearProgressIndicator(
                    progress = { (progress.percent?.div(100f) ?: 0f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun PopupWebViewDialog(
    popupState: PopupWindowState,
    webSessionViewModel: WebSessionViewModel,
) {
    BackHandler {
        if (webSessionViewModel.popupCanGoBack()) webSessionViewModel.popupGoBack()
        else webSessionViewModel.closePopup()
    }
    Dialog(
        onDismissRequest = { webSessionViewModel.closePopup() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                popupState.title.ifBlank { "新視窗" },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                SafeUrlPolicy.displayHost(popupState.webView.url),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (webSessionViewModel.popupCanGoBack()) webSessionViewModel.popupGoBack()
                                else webSessionViewModel.closePopup()
                            },
                        ) {
                            Icon(Icons.Outlined.ArrowBack, contentDescription = "返回")
                        }
                    },
                    actions = {
                        IconButton(onClick = { webSessionViewModel.closePopup() }) {
                            Icon(Icons.Outlined.Close, contentDescription = "關閉視窗")
                        }
                    },
                )
                AndroidView(
                    factory = { popupState.webView },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
