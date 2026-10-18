package com.william12233.sehuatang

import android.Manifest
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.william12233.sehuatang.ui.AppViewModel
import com.william12233.sehuatang.ui.SehuaTangApp
import com.william12233.sehuatang.ui.theme.SehuatangTheme
import com.william12233.sehuatang.web.download.DownloadRequest
import com.william12233.sehuatang.web.download.DownloadViewModel
import com.william12233.sehuatang.web.navigation.SafeUrlPolicy
import com.william12233.sehuatang.web.session.WebSessionViewModel

class MainActivity : ComponentActivity() {
    private val appViewModel by viewModels<AppViewModel>()
    private val webSessionViewModel by viewModels<WebSessionViewModel>()
    private val downloadViewModel by viewModels<DownloadViewModel>()

    private lateinit var filePickerLauncher: ActivityResultLauncher<String>
    private lateinit var requestStorageLauncher: ActivityResultLauncher<String>
    private var pendingDownload: DownloadRequest? = null
    private var receiverRegistered = false

    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            if (id >= 0L) downloadViewModel.onDownloadBroadcast(id)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)

        filePickerLauncher = registerForActivityResult(
            ActivityResultContracts.GetMultipleContents(),
        ) { uris ->
            webSessionViewModel.onFilePickerResult(uris)
        }
        requestStorageLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            val request = pendingDownload
            pendingDownload = null
            if (granted && request != null) {
                enqueueDownload(request)
            } else if (request != null) {
                Toast.makeText(this, "未取得儲存權限，下載已取消", Toast.LENGTH_SHORT).show()
            }
        }

        webSessionViewModel.requestFilePicker = {
            // SAF 不需要讀取整個儲存空間的權限，網站表單會自行處理檔案欄位。
            filePickerLauncher.launch("*/*")
        }
        webSessionViewModel.requestDownload = ::requestDownload
        webSessionViewModel.onPageVisited = appViewModel::recordVisit
        webSessionViewModel.onExternalUri = ::openExternalUri

        ContextCompat.registerReceiver(
            this,
            downloadReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        receiverRegistered = true

        setContent {
            val themeMode = appViewModel.themeMode.collectAsStateWithLifecycle().value
            SehuatangTheme(themeMode = themeMode) {
                SehuaTangApp(
                    appViewModel = appViewModel,
                    webSessionViewModel = webSessionViewModel,
                    downloadViewModel = downloadViewModel,
                    onOpenDownloads = {
                        runCatching {
                            startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS))
                        }.onFailure {
                            Toast.makeText(this, "無法開啟系統下載管理", Toast.LENGTH_SHORT).show()
                        }
                    },
                )
            }
        }
    }

    override fun onPause() {
        webSessionViewModel.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        webSessionViewModel.onResume()
    }

    override fun onDestroy() {
        if (receiverRegistered) unregisterReceiver(downloadReceiver)
        webSessionViewModel.requestFilePicker = null
        webSessionViewModel.requestDownload = null
        webSessionViewModel.onPageVisited = null
        webSessionViewModel.onExternalUri = null
        super.onDestroy()
    }

    private fun requestDownload(request: DownloadRequest) {
        if (!SafeUrlPolicy.isHttpUrl(request.url)) {
            Toast.makeText(this, "不安全的下載網址已拒絕", Toast.LENGTH_LONG).show()
            return
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingDownload = request
            requestStorageLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            enqueueDownload(request)
        }
    }

    private fun enqueueDownload(request: DownloadRequest) {
        downloadViewModel.enqueue(request).onFailure { error ->
            Toast.makeText(
                this,
                "下載無法開始：${error.message ?: "請稍後重試"}",
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    private fun openExternalUri(uri: Uri) {
        val scheme = uri.scheme?.lowercase()
        if (scheme !in setOf("tel", "mailto")) {
            Toast.makeText(this, "已阻止不受支援的外部連結", Toast.LENGTH_SHORT).show()
            return
        }
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }.onFailure {
            Toast.makeText(this, "裝置沒有可處理此連結的應用程式", Toast.LENGTH_SHORT).show()
        }
    }
}
