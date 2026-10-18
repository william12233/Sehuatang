package com.william12233.sehuatang.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.william12233.sehuatang.ui.AppViewModel
import com.william12233.sehuatang.web.navigation.SafeUrlPolicy
import com.william12233.sehuatang.web.session.WebSessionViewModel
import com.william12233.sehuatang.web.session.WebTab

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun WebPageScreen(
    tab: WebTab,
    initialUrl: String,
    appViewModel: AppViewModel,
    webSessionViewModel: WebSessionViewModel,
    onBackAtRoot: () -> Unit,
) {
    val state by webSessionViewModel.state.collectAsStateWithLifecycle()
    val currentUrl = state.url ?: webSessionViewModel.currentUrl(tab)
    val isBookmarked = appViewModel.isBookmarked(currentUrl)

    BackHandler {
        if (webSessionViewModel.canGoBack(tab)) {
            webSessionViewModel.goBack(tab)
        } else {
            onBackAtRoot()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        CenterAlignedTopAppBar(
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = state.title.ifBlank { "論壇" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = SafeUrlPolicy.displayHost(currentUrl),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            navigationIcon = {
                Row {
                    IconButton(
                        onClick = {
                            if (webSessionViewModel.canGoBack(tab)) webSessionViewModel.goBack(tab)
                            else onBackAtRoot()
                        },
                    ) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "返回")
                    }
                    IconButton(
                        enabled = webSessionViewModel.currentUrl(tab) != null,
                        onClick = { webSessionViewModel.goForward(tab) },
                    ) {
                        Icon(Icons.Outlined.ArrowForward, contentDescription = "前進")
                    }
                }
            },
            actions = {
                IconButton(
                    enabled = SafeUrlPolicy.canPersistInLocalHistory(currentUrl),
                    onClick = {
                        currentUrl?.let { url ->
                            appViewModel.toggleBookmark(
                                title = state.title,
                                url = url,
                            )
                        }
                    },
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (isBookmarked) "移除本機收藏" else "加入本機收藏",
                    )
                }
                IconButton(onClick = { webSessionViewModel.reload(tab) }) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "重新載入")
                }
            },
        )
        if (state.loading) {
            LinearProgressIndicator(
                progress = { (state.progress / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        state.externalHost?.let { host ->
            Surface(color = MaterialTheme.colorScheme.tertiaryContainer) {
                Text(
                    text = "目前頁面位於外部網域 $host；App 未注入 JavaScript 介面，Cookie 仍依網域規則處理。",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            AndroidView(
                factory = { webSessionViewModel.webViewFor(tab, initialUrl) },
                modifier = Modifier.fillMaxSize(),
            )
            state.errorMessage?.let { message ->
                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    tonalElevation = 4.dp,
                    shape = MaterialTheme.shapes.large,
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Outlined.ErrorOutline, contentDescription = null)
                        Text(
                            text = message,
                            modifier = Modifier.padding(top = 10.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        TextButton(onClick = { webSessionViewModel.reload(tab) }) {
                            Text("重試")
                        }
                    }
                }
            }
            if (state.loading && state.progress == 0) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}
