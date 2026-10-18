package com.william12233.sehuatang.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.william12233.sehuatang.data.model.LocalBookmark
import com.william12233.sehuatang.data.model.RecentVisit
import com.william12233.sehuatang.data.model.ThemeMode
import com.william12233.sehuatang.ui.SessionUiState
import com.william12233.sehuatang.ui.AsyncState
import com.william12233.sehuatang.data.model.UserProfile
import com.william12233.sehuatang.ui.latestBookmarksFirst

@Composable
fun ProfileScreen(
    bookmarks: List<LocalBookmark>,
    recentVisits: List<RecentVisit>,
    themeMode: ThemeMode,
    session: SessionUiState,
    profile: AsyncState<UserProfile>,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onOpenLogin: () -> Unit,
    onOpenThread: (String) -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenCheckIn: () -> Unit,
    onRemoveBookmark: (String) -> Unit,
    onClearHistory: () -> Unit,
    onClearSession: () -> Unit,
) {
    var showClearSessionDialog by rememberSaveable { mutableStateOf(false) }
    val remoteProfile = (profile as? AsyncState.Data)?.value
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("我的", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "會員資料、積分與權限以論壇實際回應為準；這裡不嵌入網站會員頁。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Outlined.AccountCircle, contentDescription = null)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(remoteProfile?.username ?: session.username ?: "尚未登入", fontWeight = FontWeight.SemiBold)
                        Text(
                            listOfNotNull(
                                if (session.isLoggedIn) "原生 HTTP 工作階段已建立" else "登入後可依論壇權限使用更多功能",
                                remoteProfile?.group,
                                remoteProfile?.points?.let { "積分 $it" },
                            ).joinToString("  ·  "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (!session.isLoggedIn) {
                        OutlinedButton(onClick = onOpenLogin) {
                            Icon(Icons.Outlined.Login, contentDescription = null)
                            Spacer(Modifier.width(5.dp))
                            Text("登入")
                        }
                    }
                }
            }
        }
        item { Text("原生功能", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        item {
            ActionRow(Icons.Outlined.Download, "下載管理", "查看 Android 原生下載進度與檔案", onOpenDownloads)
        }
        item {
            ActionRow(
                Icons.Outlined.CheckCircleOutline,
                "簽到",
                "使用論壇實際表單；需要時由你明確送出",
                onOpenCheckIn,
            )
        }
        item {
            Text("收藏", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        if (bookmarks.isEmpty()) {
            item { Text("尚無收藏。閱讀文章時可在 App 工具列加入收藏。", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(bookmarks.latestBookmarksFirst(), key = { it.url }) { bookmark ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(modifier = Modifier.weight(1f), onClick = { onOpenThread(bookmark.url) }) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(bookmark.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "論壇文章",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    IconButton(onClick = { onRemoveBookmark(bookmark.url) }) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "移除收藏")
                    }
                }
            }
        }
        item { Text("設定與資料", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Settings, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("外觀", fontWeight = FontWeight.SemiBold)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        ThemeChip("系統", ThemeMode.SYSTEM, themeMode, onThemeModeChanged)
                        ThemeChip("淺色", ThemeMode.LIGHT, themeMode, onThemeModeChanged)
                        ThemeChip("深色", ThemeMode.DARK, themeMode, onThemeModeChanged)
                    }
                    TextButton(onClick = onClearHistory) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = null)
                        Spacer(Modifier.width(5.dp))
                        Text("清除閱讀歷史（${recentVisits.size}）")
                    }
                    TextButton(onClick = { showClearSessionDialog = true }) {
                        Text("清除登入工作階段")
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Outlined.Info, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "本 App 是獨立第三方客戶端。網站驗證、權限、積分與附件限制由伺服器決定；一般閱讀不使用 WebView。",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }

    if (showClearSessionDialog) {
        AlertDialog(
            onDismissRequest = { showClearSessionDialog = false },
            title = { Text("清除登入工作階段？") },
            text = { Text("只會清除原生 HTTP Cookie，不會刪除收藏、閱讀紀錄或下載檔案。") },
            confirmButton = {
                TextButton(onClick = { showClearSessionDialog = false; onClearSession() }) { Text("清除") }
            },
            dismissButton = { TextButton(onClick = { showClearSessionDialog = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun ActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ThemeChip(label: String, value: ThemeMode, selected: ThemeMode, onSelected: (ThemeMode) -> Unit) {
    FilterChip(selected = selected == value, onClick = { onSelected(value) }, label = { Text(label) })
}
