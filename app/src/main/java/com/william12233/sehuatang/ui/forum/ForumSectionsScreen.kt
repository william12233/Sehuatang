package com.william12233.sehuatang.ui.forum

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.william12233.sehuatang.data.model.ForumHome
import com.william12233.sehuatang.data.model.ForumSection
import com.william12233.sehuatang.ui.AsyncState
import com.william12233.sehuatang.ui.components.LoadingThreadSkeleton
import com.william12233.sehuatang.ui.components.NativeFailureCard
import com.william12233.sehuatang.ui.components.SectionCard

@Composable
fun ForumSectionsScreen(
    state: AsyncState<ForumHome>,
    onRetry: () -> Unit,
    onOpenSection: (ForumSection) -> Unit,
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
            val sections = state.value.sections
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text("論壇版區", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "版區資料由論壇首頁 HTTP 回應解析而來。",
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(sections, key = { it.id }) { section ->
                    SectionCard(section) { onOpenSection(section) }
                }
                if (sections.isEmpty()) {
                    item {
                        Text(
                            "目前回應沒有可辨識的版區節點。",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
