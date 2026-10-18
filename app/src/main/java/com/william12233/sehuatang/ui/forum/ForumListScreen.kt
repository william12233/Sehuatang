package com.william12233.sehuatang.ui.forum

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.william12233.sehuatang.data.model.ForumThreadPage
import com.william12233.sehuatang.ui.AsyncState
import com.william12233.sehuatang.ui.components.LoadingThreadSkeleton
import com.william12233.sehuatang.ui.components.NativeFailureCard
import com.william12233.sehuatang.ui.components.ThreadCard

@Composable
fun ForumListScreen(
    state: AsyncState<ForumThreadPage>,
    onRetry: () -> Unit,
    onOpenThread: (String) -> Unit,
    onNewThread: (String) -> Unit,
    onPageChange: (Int) -> Unit,
    onVerification: () -> Unit,
) {
    when (state) {
        AsyncState.Idle, AsyncState.Loading -> LoadingThreadSkeleton(Modifier.padding(18.dp))
        is AsyncState.Error -> NativeFailureCard(
            modifier = Modifier.padding(18.dp),
            failure = state.failure,
            onRetry = onRetry,
            onVerification = if (state.failure is com.william12233.sehuatang.data.model.ForumFailure.Verification) {
                onVerification
            } else null,
        )

        is AsyncState.Data -> {
            val page = state.value
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        "文章列表",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    Text(
                        "第 ${page.page} 頁 · 共 ${page.totalPages} 頁",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = { onNewThread(page.sourceUrl) }, modifier = Modifier.padding(top = 10.dp)) {
                        Text("發表新主題")
                    }
                    if (page.totalPages > 1) {
                        Row(modifier = Modifier.padding(top = 4.dp)) {
                            TextButton(
                                enabled = page.page > 1,
                                onClick = { onPageChange(page.page - 1) },
                            ) { Text("上一頁") }
                            Spacer(Modifier.width(8.dp))
                            TextButton(
                                enabled = page.page < page.totalPages,
                                onClick = { onPageChange(page.page + 1) },
                            ) { Text("下一頁") }
                        }
                    }
                }
                items(page.threads, key = { it.id }) { thread ->
                    ThreadCard(thread) { onOpenThread(thread.url) }
                }
            }
        }
    }
}
