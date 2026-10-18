package com.william12233.sehuatang.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.william12233.sehuatang.data.model.CheckInForm
import com.william12233.sehuatang.data.model.CheckInOutcome
import com.william12233.sehuatang.data.model.ForumFailure
import com.william12233.sehuatang.ui.AsyncState
import com.william12233.sehuatang.ui.components.NativeFailureCard

@Composable
fun CheckInScreen(
    formState: AsyncState<CheckInForm>,
    submitState: AsyncState<CheckInOutcome>,
    onLoad: () -> Unit,
    onSubmit: () -> Unit,
    onVerification: () -> Unit,
    onBack: () -> Unit,
) {
    LaunchedEffect(Unit) {
        if (formState is AsyncState.Idle) onLoad()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("論壇簽到", style = MaterialTheme.typography.headlineSmall)
        Text(
            "簽到只會使用論壇回應中的 action、hidden 欄位與提交欄位；必須由你明確按下按鈕，App 不會自動簽到。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        when (formState) {
            AsyncState.Idle, AsyncState.Loading -> {
                if (formState is AsyncState.Loading) CircularProgressIndicator()
            }

            is AsyncState.Error -> NativeFailureCard(
                failure = formState.failure,
                onRetry = onLoad,
                onVerification = if (formState.failure is ForumFailure.Verification) onVerification else null,
            )

            is AsyncState.Data -> CheckInFormCard(
                form = formState.value,
                submitState = submitState,
                onSubmit = onSubmit,
                onVerification = onVerification,
            )
        }
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("返回") }
    }
}

@Composable
private fun CheckInFormCard(
    form: CheckInForm,
    submitState: AsyncState<CheckInOutcome>,
    onSubmit: () -> Unit,
    onVerification: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("已取得論壇簽到表單", style = MaterialTheme.typography.titleMedium)
            Text(
                "提交方式：${form.method} · ${form.fields.size} 個伺服器欄位",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (form.requiresCaptcha) {
                Text(
                    "此表單包含驗證碼。若伺服器拒絕提交，請依錯誤提示完成隔離驗證；App 不會自動處理驗證碼。",
                    color = MaterialTheme.colorScheme.error,
                )
            }
            when (submitState) {
                AsyncState.Idle -> Unit
                AsyncState.Loading -> CircularProgressIndicator()
                is AsyncState.Data -> Text(
                    submitState.value.message,
                    color = MaterialTheme.colorScheme.primary,
                )
                is AsyncState.Error -> NativeFailureCard(
                    failure = submitState.failure,
                    onRetry = onSubmit,
                    onVerification = if (submitState.failure is ForumFailure.Verification) onVerification else null,
                )
            }
            Button(
                onClick = onSubmit,
                enabled = submitState !is AsyncState.Loading,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(form.label) }
        }
    }
}
