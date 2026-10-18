package com.william12233.sehuatang.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.william12233.sehuatang.data.model.ForumFailure
import com.william12233.sehuatang.data.model.SearchResult
import com.william12233.sehuatang.ui.AsyncState
import com.william12233.sehuatang.ui.components.LoadingThreadSkeleton
import com.william12233.sehuatang.ui.components.NativeFailureCard
import com.william12233.sehuatang.ui.components.ThreadCard

@Composable
fun SearchScreen(
    state: AsyncState<SearchResult>,
    onSearch: (String) -> Unit,
    onOpenThread: (String) -> Unit,
    onVerification: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("搜尋論壇文章") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
            )
            IconButton(onClick = { onSearch(query) }) {
                Icon(Icons.Outlined.Search, contentDescription = "搜尋")
            }
        }
        when (state) {
            AsyncState.Idle -> SearchHint()
            AsyncState.Loading -> LoadingThreadSkeleton(Modifier.padding(16.dp))
            is AsyncState.Error -> NativeFailureCard(
                modifier = Modifier.padding(16.dp),
                failure = state.failure,
                onRetry = { onSearch(query) },
                onVerification = if (state.failure is ForumFailure.Verification) onVerification else null,
            )

            is AsyncState.Data -> {
                val result = state.value
                if (result.threads.isEmpty()) {
                    SearchHint(text = "沒有找到論壇文章")
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item {
                            Text(
                                "搜尋結果 · 第 ${result.page} / ${result.totalPages} 頁",
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        items(result.threads, key = { it.id }) { thread ->
                            ThreadCard(thread) { onOpenThread(thread.url) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchHint(text: String = "輸入關鍵字後搜尋論壇內容") {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(34.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium)
        Text(
            "搜尋表單與欄位名稱會先從論壇回應解析，App 不假設不存在的 JSON API。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
