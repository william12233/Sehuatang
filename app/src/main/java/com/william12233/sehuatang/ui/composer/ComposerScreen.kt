package com.william12233.sehuatang.ui.composer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.william12233.sehuatang.data.model.ForumFailure
import com.william12233.sehuatang.data.model.PostForm
import com.william12233.sehuatang.ui.AsyncState
import com.william12233.sehuatang.ui.components.NativeFailureCard

@Composable
fun ComposerScreen(
    formState: AsyncState<PostForm>,
    submitState: AsyncState<Unit>,
    onLoadForm: () -> Unit,
    onSubmit: (String?, String) -> Unit,
    onVerification: () -> Unit,
    onPickAttachments: () -> Unit,
    selectedAttachmentLabel: String,
    onBack: () -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var content by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        if (formState is AsyncState.Idle) onLoadForm()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("原生編輯器", style = MaterialTheme.typography.headlineSmall)
        Text(
            "送出前會使用論壇回應中的 action、hidden token、formhash 與欄位名稱；未取得真實表單時不會猜測提交。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        when (formState) {
            AsyncState.Idle, AsyncState.Loading -> {
                if (formState is AsyncState.Loading) CircularProgressIndicator()
            }

            is AsyncState.Error -> NativeFailureCard(
                failure = formState.failure,
                onRetry = onLoadForm,
                onVerification = if (formState.failure is ForumFailure.Verification) onVerification else null,
            )

            is AsyncState.Data -> {
                val form = formState.value
                if (form.titleField != null) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("標題") },
                        singleLine = true,
                    )
                } else {
                    Text("目前取得的論壇表單沒有標題欄位；這個編輯器會依伺服器表單限制送出。", style = MaterialTheme.typography.bodySmall)
                }
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 220.dp),
                    label = { Text("正文") },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onPickAttachments) {
                        Icon(Icons.Outlined.AttachFile, contentDescription = null)
                        Text("圖片／附件")
                    }
                    OutlinedButton(onClick = { /* draft is held by rememberSaveable */ }) {
                        Icon(Icons.Outlined.Save, contentDescription = null)
                        Text("保存草稿")
                    }
                }
                if (selectedAttachmentLabel.isNotBlank()) {
                    Text("已選擇：$selectedAttachmentLabel", style = MaterialTheme.typography.bodySmall)
                }
                if (form.requiresCaptcha) {
                    Text(
                        "論壇表單要求驗證碼。請先完成隔離驗證；App 不會自動處理驗證碼。",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                when (submitState) {
                    is AsyncState.Error -> NativeFailureCard(
                        failure = submitState.failure,
                        onRetry = { onSubmit(title.takeIf { it.isNotBlank() }, content) },
                        onVerification = if (submitState.failure is ForumFailure.Verification) onVerification else null,
                    )
                    AsyncState.Loading -> CircularProgressIndicator()
                    else -> Unit
                }
                Button(
                    onClick = { onSubmit(title.takeIf { it.isNotBlank() }, content) },
                    enabled = content.isNotBlank() && submitState !is AsyncState.Loading,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("送出") }
            }
        }
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("取消") }
    }
}
