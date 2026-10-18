package com.william12233.sehuatang.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.william12233.sehuatang.data.model.ForumHome
import com.william12233.sehuatang.data.model.ForumSection
import com.william12233.sehuatang.data.model.ForumThread
import com.william12233.sehuatang.data.model.RecentVisit
import com.william12233.sehuatang.ui.AsyncState
import com.william12233.sehuatang.ui.components.LoadingThreadSkeleton
import com.william12233.sehuatang.ui.components.NativeFailureCard
import com.william12233.sehuatang.ui.components.SectionCard
import com.william12233.sehuatang.ui.components.ThreadCard

@Composable
fun HomeScreen(
    home: AsyncState<ForumHome>,
    recentVisits: List<RecentVisit>,
    onRefresh: () -> Unit,
    onOpenSection: (ForumSection) -> Unit,
    onOpenThread: (ForumThread) -> Unit,
    onOpenRecent: (String) -> Unit,
    onOpenLogin: () -> Unit,
    onOpenVerification: () -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("色花堂閱讀器", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "以原生介面閱讀論壇資料。網站只提供資料與服務，不會把網站頁面嵌入 App。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(
                            onClick = onRefresh,
                            label = { Text("重新同步") },
                            leadingIcon = { Icon(Icons.Outlined.Refresh, contentDescription = null) },
                        )
                        OutlinedButton(onClick = onOpenLogin) {
                            Icon(Icons.Outlined.Login, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("登入")
                        }
                    }
                }
            }
        }

        when (home) {
            AsyncState.Idle, AsyncState.Loading -> item { LoadingThreadSkeleton() }
            is AsyncState.Error -> item {
                NativeFailureCard(
                    failure = home.failure,
                    onRetry = onRefresh,
                    onVerification = if (home.failure is com.william12233.sehuatang.data.model.ForumFailure.Verification) {
                        onOpenVerification
                    } else null,
                )
            }

            is AsyncState.Data -> {
                val value = home.value
                if (value.sections.isNotEmpty()) {
                    item {
                        Text("版區", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(value.sections.take(12), key = { it.id }) { section ->
                                SectionMiniCard(section, onClick = { onOpenSection(section) })
                            }
                        }
                    }
                }
                if (value.latestThreads.isNotEmpty()) {
                    item {
                        Text("最新文章", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    items(value.latestThreads, key = { it.id }) { thread ->
                        ThreadCard(thread, onClick = { onOpenThread(thread) })
                    }
                }
                if (value.popularThreads.isNotEmpty()) {
                    item {
                        Text("熱門文章", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    itemsIndexed(
                        value.popularThreads.take(8),
                        key = { _, thread -> "popular-${thread.id}" },
                    ) { _, thread -> ThreadCard(thread, onClick = { onOpenThread(thread) }) }
                }
                if (value.sections.isEmpty() && value.latestThreads.isEmpty() && value.popularThreads.isEmpty()) {
                    item { EmptyHomeState(onRefresh) }
                }
            }
        }

        if (recentVisits.isNotEmpty()) {
            item {
                Text("最近閱讀", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            items(recentVisits.take(5), key = { it.url }) { visit ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenRecent(visit.url) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(visit.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "從本機閱讀紀錄開啟",
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
private fun SectionMiniCard(section: ForumSection, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .width(150.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(section.name, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(section.threadCount?.let { "文章 $it" }, section.postCount?.let { "回覆 $it" })
                    .joinToString(" · ")
                    .ifBlank { "版區" },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyHomeState(onRefresh: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("論壇目前沒有可顯示的資料", style = MaterialTheme.typography.titleMedium)
        Text("App 不會用假文章填補內容。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = onRefresh) { Text("重新載入") }
    }
}
