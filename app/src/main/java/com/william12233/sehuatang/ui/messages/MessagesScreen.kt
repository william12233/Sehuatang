package com.william12233.sehuatang.ui.messages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MarkEmailUnread
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.william12233.sehuatang.data.model.ForumFailure
import com.william12233.sehuatang.data.model.ForumMessage
import com.william12233.sehuatang.ui.AsyncState
import com.william12233.sehuatang.ui.components.LoadingThreadSkeleton
import com.william12233.sehuatang.ui.components.NativeFailureCard

@Composable
fun MessagesScreen(
    state: AsyncState<List<ForumMessage>>,
    onRetry: () -> Unit,
    onLogin: () -> Unit,
    onVerification: () -> Unit,
    onOpenMessage: (ForumMessage) -> Unit,
) {
    when (state) {
        AsyncState.Idle, AsyncState.Loading -> LoadingThreadSkeleton(Modifier.padding(18.dp))
        is AsyncState.Error -> {
            if (state.failure == ForumFailure.AuthenticationRequired) {
                TextButton(onClick = onLogin, modifier = Modifier.padding(18.dp)) { Text("請先登入論壇以查看私訊") }
            } else {
                NativeFailureCard(
                    modifier = Modifier.padding(18.dp),
                    failure = state.failure,
                    onRetry = onRetry,
                    onVerification = if (state.failure is ForumFailure.Verification) onVerification else null,
                )
            }
        }

        is AsyncState.Data -> {
            val messages = state.value
            if (messages.isEmpty()) {
                Text(
                    "目前沒有可顯示的私訊。",
                    modifier = Modifier.padding(20.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(messages, key = { it.id }) { message ->
                        MessageCard(message) { onOpenMessage(message) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageCard(message: ForumMessage, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.MarkEmailUnread, contentDescription = null)
            androidx.compose.foundation.layout.Spacer(Modifier.padding(horizontal = 5.dp))
            androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f), horizontalAlignment = androidx.compose.ui.Alignment.Start) {
                Text(message.author, fontWeight = FontWeight.SemiBold)
                Text(message.preview, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    message.createdAt,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
