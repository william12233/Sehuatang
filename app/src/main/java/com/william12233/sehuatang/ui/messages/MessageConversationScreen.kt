package com.william12233.sehuatang.ui.messages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.william12233.sehuatang.data.model.ConversationMessage
import com.william12233.sehuatang.data.model.ForumConversation
import com.william12233.sehuatang.data.model.ForumFailure
import com.william12233.sehuatang.data.model.MessageReplyForm
import com.william12233.sehuatang.ui.AsyncState
import com.william12233.sehuatang.ui.components.NativeFailureCard
import com.william12233.sehuatang.ui.components.userMessage

@Composable
fun MessageConversationScreen(
    conversationState: AsyncState<ForumConversation>,
    replyFormState: AsyncState<MessageReplyForm>,
    submitState: AsyncState<Unit>,
    onRetry: () -> Unit,
    onSubmit: (String) -> Unit,
    onVerification: () -> Unit,
) {
    var draft by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(submitState) {
        if (submitState is AsyncState.Data) draft = ""
    }
    when (conversationState) {
        AsyncState.Idle, AsyncState.Loading -> Column(Modifier.padding(18.dp)) { CircularProgressIndicator() }
        is AsyncState.Error -> NativeFailureCard(
            modifier = Modifier.padding(18.dp),
            failure = conversationState.failure,
            onRetry = onRetry,
            onVerification = if (conversationState.failure is ForumFailure.Verification) onVerification else null,
        )
        is AsyncState.Data -> {
            val conversation = conversationState.value
            Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item { Text(conversation.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                    items(conversation.messages, key = { it.id }) { message -> ConversationMessageCard(message) }
                }
                if (replyFormState is AsyncState.Error) {
                    Text(
                        replyFormState.failure.userMessage(),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("輸入訊息") },
                        enabled = replyFormState is AsyncState.Data,
                    )
                    Button(
                        onClick = { onSubmit(draft) },
                        enabled = draft.isNotBlank() && replyFormState is AsyncState.Data && submitState !is AsyncState.Loading,
                    ) { Text("送出") }
                }
            }
        }
    }
}

@Composable
private fun ConversationMessageCard(message: ConversationMessage) {
    androidx.compose.material3.Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(message.author, fontWeight = FontWeight.SemiBold)
            Text(message.text, style = MaterialTheme.typography.bodyLarge)
            Text(message.createdAt, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
