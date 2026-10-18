package com.william12233.sehuatang.ui.verification

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import com.william12233.sehuatang.web.navigation.SafeUrlPolicy

/**
 * 僅供站點驗證使用的隔離 WebView。一般首頁、版區、文章、搜尋與我的頁面不會進入這裡。
 */
@Composable
fun VerificationScreen(
    url: String,
    message: String,
    onComplete: (String?) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    var webView by remember { mutableStateOf<WebView?>(null) }
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "只在這個隔離頁完成必要的站點驗證；完成後按下方按鈕回到原生 App。",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            message,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AndroidView(
            factory = { context -> createVerificationWebView(context, url).also { webView = it } },
            modifier = Modifier.weight(1f),
        )
        Button(
            onClick = {
                CookieManager.getInstance().flush()
                val cookie = CookieManager.getInstance().getCookie(url)
                onComplete(cookie)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) { Text("完成驗證並回到 App") }
    }
    DisposableEffect(Unit) {
        onDispose {
            webView?.let {
                it.stopLoading()
                it.destroy()
            }
            webView = null
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun createVerificationWebView(context: Context, url: String): WebView =
    WebView(context).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.setSupportMultipleWindows(false)
        CookieManager.getInstance().setAcceptCookie(true)
        webViewClient = WebViewClient()
        if (SafeUrlPolicy.isHttpUrl(url)) loadUrl(url)
    }
