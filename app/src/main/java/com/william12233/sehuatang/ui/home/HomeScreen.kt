package com.william12233.sehuatang.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Web
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.william12233.sehuatang.data.model.RecentVisit
import com.william12233.sehuatang.web.navigation.ForumUrls
import com.william12233.sehuatang.web.session.WebTab

@Composable
fun HomeScreen(
    recentVisits: List<RecentVisit>,
    onOpenWeb: (WebTab, String) -> Unit,
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "色花堂",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "以更順手的 Android 介面，連回論壇原本的登入、閱讀、發文與附件流程。",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "第三方客戶端，非色花堂官方產品",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "今日開始",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "登入、驗證碼、發帖、回覆、簽到與附件驗證都由論壇網站處理；App 不會保存明文密碼，也不會繞過人機驗證。",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onOpenWeb(WebTab.FORUM, ForumUrls.HOME) }) {
                        Icon(Icons.Outlined.Forum, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("開啟論壇")
                    }
                    OutlinedButton(onClick = { onOpenWeb(WebTab.PROFILE, ForumUrls.LOGIN) }) {
                        Icon(Icons.Outlined.Login, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("登入")
                    }
                }
            }
        }

        Text(
            text = "常用入口",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        QuickActionRow(
            firstIcon = Icons.Outlined.CalendarMonth,
            firstText = "每日簽到",
            onFirst = { onOpenWeb(WebTab.PROFILE, ForumUrls.SIGN_IN) },
            secondIcon = Icons.Outlined.ChatBubbleOutline,
            secondText = "站內訊息",
            onSecond = { onOpenWeb(WebTab.MESSAGES, ForumUrls.MESSAGES) },
        )
        QuickActionRow(
            firstIcon = Icons.Outlined.Search,
            firstText = "論壇搜尋",
            onFirst = { onOpenWeb(WebTab.SEARCH, ForumUrls.SEARCH) },
            secondIcon = Icons.Outlined.Web,
            secondText = "我的主題",
            onSecond = { onOpenWeb(WebTab.PROFILE, ForumUrls.MY_GUIDE) },
        )

        if (recentVisits.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "最近瀏覽",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "僅儲存在本機",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            recentVisits.take(5).forEach { visit ->
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onOpenWeb(WebTab.FORUM, visit.url) },
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = visit.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = visit.url,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionRow(
    firstIcon: androidx.compose.ui.graphics.vector.ImageVector,
    firstText: String,
    onFirst: () -> Unit,
    secondIcon: androidx.compose.ui.graphics.vector.ImageVector,
    secondText: String,
    onSecond: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        QuickAction(
            modifier = Modifier.weight(1f),
            icon = firstIcon,
            text = firstText,
            onClick = onFirst,
        )
        QuickAction(
            modifier = Modifier.weight(1f),
            icon = secondIcon,
            text = secondText,
            onClick = onSecond,
        )
    }
}

@Composable
private fun QuickAction(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit,
) {
    OutlinedButton(modifier = modifier.height(72.dp), onClick = onClick) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.height(4.dp))
            Text(text)
        }
    }
}
