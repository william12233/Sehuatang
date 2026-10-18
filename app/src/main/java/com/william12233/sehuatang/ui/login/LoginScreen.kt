package com.william12233.sehuatang.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.william12233.sehuatang.data.model.ForumFailure
import com.william12233.sehuatang.data.model.LoginCredentials
import com.william12233.sehuatang.data.model.LoginForm
import com.william12233.sehuatang.ui.AsyncState
import com.william12233.sehuatang.ui.components.NativeFailureCard

@Composable
fun LoginScreen(
    state: AsyncState<LoginForm>,
    onLoad: () -> Unit,
    onLogin: (LoginCredentials) -> Unit,
    onVerification: () -> Unit,
    onBack: () -> Unit,
) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var securityAnswer by rememberSaveable { mutableStateOf("") }
    var rememberMe by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        if (state is AsyncState.Idle) onLoad()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(PaddingValues(horizontal = 22.dp, vertical = 20.dp)),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("登入論壇", style = MaterialTheme.typography.headlineSmall)
        Text(
            "帳號與密碼只在送出登入請求時使用，不會寫入本機。表單欄位與隱藏 token 由論壇登入頁實際提供。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        when (state) {
            AsyncState.Idle, AsyncState.Loading -> {
                if (state is AsyncState.Loading) CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            is AsyncState.Error -> NativeFailureCard(
                failure = state.failure,
                onRetry = onLoad,
                onVerification = if (state.failure is ForumFailure.Verification) onVerification else null,
            )

            is AsyncState.Data -> {
                val form = state.value
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("帳號") },
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("密碼") },
                    visualTransformation = PasswordVisualTransformation(),
                )
                if (form.securityQuestionField != null) {
                    OutlinedTextField(
                        value = securityAnswer,
                        onValueChange = { securityAnswer = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("安全提問答案") },
                    )
                }
                androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = rememberMe, onCheckedChange = { rememberMe = it })
                    Text("保持登入")
                }
                form.captchaDescription?.let {
                    Text(
                        "論壇登入需要額外驗證：$it。請使用隔離驗證頁完成，不會在 App 內繞過。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Button(
                    onClick = {
                        onLogin(LoginCredentials(username, password, securityAnswer.takeIf { it.isNotBlank() }, rememberMe))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = username.isNotBlank() && password.isNotBlank(),
                ) { Text("登入") }
            }
        }
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("返回") }
    }
}
