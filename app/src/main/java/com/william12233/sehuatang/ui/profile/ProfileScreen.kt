package com.william12233.sehuatang.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
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
import com.william12233.sehuatang.ui.latestBookmarksFirst
import com.william12233.sehuatang.web.navigation.ForumUrls
import com.william12233.sehuatang.web.session.WebTab

@Composable
fun ProfileScreen(
    bookmarks: List<LocalBookmark>,
    recentVisits: List<RecentVisit>,
    themeMode: ThemeMode,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onOpenWeb: (WebTab, String) -> Unit,
    onOpenDownloads: () -> Unit,
    onRemoveBookmark: (String) -> Unit,
    onClearHistory: () -> Unit,
    onClearWebsiteSession: () -> Unit,
) {
    var showClearSessionDialog by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.padding(horizontal = 20.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                text = "我的",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "會員資料與權限仍以論壇網站工作階段為準。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.AccountCircle, contentDescription = null)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("論壇會員中心", fontWeight = FontWeight.SemiBold)
                            Text(
                                "登入後可使用發帖、回覆、私訊、積分與下載權限",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    OutlinedButton(onClick = { onOpenWeb(WebTab.PROFILE, ForumUrls.LOGIN) }) {
                        Text("登入／會員中心")
                    }
                }
            }
        }

        item {
            Text("論壇工具", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        item {
            ProfileAction(
                icon = Icons.Outlined.CalendarMonth,
                title = "每日簽到",
                subtitle = "開啟論壇原有簽到頁，狀態以網站顯示為準",
                onClick = { onOpenWeb(WebTab.PROFILE, ForumUrls.SIGN_IN) },
            )
        }
        item {
            ProfileAction(
                icon = Icons.Outlined.NotificationsNone,
                title = "站內通知",
                subtitle = "讀取會員通知與未讀狀態",
                onClick = { onOpenWeb(WebTab.PROFILE, ForumUrls.NOTICES) },
            )
        }
        item {
            ProfileAction(
                icon = Icons.Outlined.Forum,
                title = "我的主題",
                subtitle = "由論壇頁面提供實際主題與權限",
                onClick = { onOpenWeb(WebTab.PROFILE, ForumUrls.MY_GUIDE) },
            )
        }
        item {
            ProfileAction(
                icon = Icons.Outlined.Download,
                title = "下載紀錄",
                subtitle = "開啟 Android 系統下載管理，查看或開啟已完成檔案",
                onClick = onOpenDownloads,
            )
        }

        item {
            Text("本機收藏", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        if (bookmarks.isEmpty()) {
            item {
                Text(
                    "尚無收藏。閱讀論壇頁面時，可使用上方工具列的星號儲存。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(bookmarks.latestBookmarksFirst(), key = { it.url }) { bookmark ->
                LocalItemRow(
                    title = bookmark.title,
                    url = bookmark.url,
                    trailing = {
                        IconButton(onClick = { onRemoveBookmark(bookmark.url) }) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "移除收藏")
                        }
                    },
                    onClick = { onOpenWeb(WebTab.FORUM, bookmark.url) },
                )
            }
        }

        item {
            Text("外觀與資料", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        item {
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Tune, contentDescription = null)
                        Spacer(Modifier.width(10.dp))
                        Text("主題模式", fontWeight = FontWeight.SemiBold)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeChip("跟隨系統", ThemeMode.SYSTEM, themeMode, onThemeModeChanged)
                        ThemeChip("淺色", ThemeMode.LIGHT, themeMode, onThemeModeChanged)
                        ThemeChip("深色", ThemeMode.DARK, themeMode, onThemeModeChanged)
                    }
                    TextButton(onClick = onClearHistory) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("清除最近瀏覽")
                    }
                    TextButton(onClick = { showClearSessionDialog = true }) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("清除網站登入工作階段")
                    }
                    Text(
                        "清除網站資料（Cookie／登入工作階段）請使用下方網站頁面或系統 App 資料管理；本 App 不會把密碼保存到本機。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Outlined.Info, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "本 App 是獨立第三方客戶端，不代表或冒充論壇官方。人機驗證、會員權限、積分獎勵與下載限制均由網站決定。",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }

    if (showClearSessionDialog) {
        AlertDialog(
            onDismissRequest = { showClearSessionDialog = false },
            title = { Text("清除網站登入工作階段？") },
            text = {
                Text("這會清除 WebView Cookie、網站儲存資料、快取與網頁歷史；不會刪除本機收藏或 Android 下載檔案。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearSessionDialog = false
                        onClearWebsiteSession()
                    },
                ) { Text("清除") }
            },
            dismissButton = {
                TextButton(onClick = { showClearSessionDialog = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun ProfileAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    TextButton(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null)
    }
}

@Composable
private fun LocalItemRow(
    title: String,
    url: String,
    trailing: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TextButton(modifier = Modifier.weight(1f), onClick = onClick) {
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    url,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        trailing()
    }
}

@Composable
private fun ThemeChip(
    label: String,
    value: ThemeMode,
    selected: ThemeMode,
    onSelected: (ThemeMode) -> Unit,
) {
    FilterChip(
        selected = value == selected,
        onClick = { onSelected(value) },
        label = { Text(label) },
    )
}
