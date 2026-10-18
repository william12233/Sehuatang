package com.william12233.sehuatang.web.session

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.lifecycle.AndroidViewModel
import com.william12233.sehuatang.web.download.DownloadRequest
import com.william12233.sehuatang.web.navigation.SafeUrlPolicy
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

enum class WebTab {
    FORUM,
    SEARCH,
    MESSAGES,
    PROFILE,
}

data class WebSessionState(
    val tab: WebTab? = null,
    val title: String = "論壇",
    val url: String? = null,
    val loading: Boolean = false,
    val progress: Int = 0,
    val errorMessage: String? = null,
    val externalHost: String? = null,
)

data class PopupWindowState(
    val webView: WebView,
    val title: String = "新視窗",
)

sealed interface WebEvent {
    data class Message(val text: String) : WebEvent
}

/**
 * 管理每個主要分頁的一個 WebView 實例，切換 Compose 分頁時保留網頁歷史與閱讀捲動位置。
 * WebView 僅以原站流程執行 JavaScript，不注入會員密碼、Cookie 或自製驗證介面。
 */
@Suppress("StaticFieldLeak")
class WebSessionViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val webViews = mutableMapOf<WebTab, WebView>()
    private val routeOverrides = mutableMapOf<WebTab, String>()
    private var activeTab: WebTab? = null
    private var fileChooserCallback: ValueCallback<Array<Uri>>? = null
    private var popupWebView: WebView? = null

    private val _state = MutableStateFlow(WebSessionState())
    val state = _state.asStateFlow()

    private val _popup = MutableStateFlow<PopupWindowState?>(null)
    val popup = _popup.asStateFlow()

    private val _events = MutableSharedFlow<WebEvent>(extraBufferCapacity = 16)
    val events = _events.asSharedFlow()

    /** 由 Activity 接上 SAF 檔案選擇器。 */
    var requestFilePicker: ((allowMultiple: Boolean) -> Unit)? = null

    /** 由 Activity 接上 DownloadManager 與必要的舊版儲存權限流程。 */
    var requestDownload: ((DownloadRequest) -> Unit)? = null

    /** 只回報可安全保留的論壇頁面，不包含登入及敏感查詢參數。 */
    var onPageVisited: ((title: String, url: String) -> Unit)? = null

    /** 由 Activity 處理 tel/mailto 等非網頁連結。 */
    var onExternalUri: ((Uri) -> Unit)? = null

    fun open(tab: WebTab, url: String) {
        if (!SafeUrlPolicy.isHttpUrl(url)) {
            emitMessage("此頁面網址格式不受支援")
            return
        }
        activeTab = tab
        routeOverrides[tab] = url
        webViews[tab]?.let { webView ->
            if (webView.url != url) webView.loadUrl(url)
        }
    }

    fun webViewFor(tab: WebTab, initialUrl: String): WebView {
        activeTab = tab
        val webView = webViews.getOrPut(tab) { createWebView(tab) }
        val requestedUrl = routeOverrides.remove(tab)
        val targetUrl = requestedUrl ?: initialUrl
        if (webView.url.isNullOrBlank()) {
            webView.loadUrl(targetUrl)
        } else if (requestedUrl != null && webView.url != requestedUrl) {
            webView.loadUrl(requestedUrl)
        }
        publishStateFor(tab, webView)
        return webView
    }

    fun canGoBack(tab: WebTab): Boolean = webViews[tab]?.canGoBack() == true

    fun goBack(tab: WebTab) {
        webViews[tab]?.takeIf { it.canGoBack() }?.goBack()
    }

    fun goForward(tab: WebTab) {
        webViews[tab]?.takeIf { it.canGoForward() }?.goForward()
    }

    fun reload(tab: WebTab) {
        webViews[tab]?.reload()
    }

    fun currentUrl(tab: WebTab): String? = webViews[tab]?.url

    fun currentTitle(tab: WebTab): String = webViews[tab]?.title.orEmpty()

    fun closePopup() {
        val view = popupWebView ?: return
        popupWebView = null
        _popup.value = null
        destroyWebView(view)
    }

    fun popupCanGoBack(): Boolean = popupWebView?.canGoBack() == true

    fun popupGoBack() {
        popupWebView?.takeIf { it.canGoBack() }?.goBack()
    }

    fun onFilePickerResult(uris: List<Uri>?) {
        fileChooserCallback?.onReceiveValue(uris?.toTypedArray())
        fileChooserCallback = null
    }

    fun onPause() {
        webViews.values.forEach(WebView::onPause)
        popupWebView?.onPause()
        CookieManager.getInstance().flush()
    }

    fun onResume() {
        webViews.values.forEach(WebView::onResume)
        popupWebView?.onResume()
    }

    fun clearWebsiteSession(onComplete: (() -> Unit)? = null) {
        CookieManager.getInstance().removeAllCookies {
            CookieManager.getInstance().flush()
            WebStorage.getInstance().deleteAllData()
            webViews.values.forEach { webView ->
                webView.clearCache(true)
                webView.clearHistory()
                webView.clearFormData()
            }
            onComplete?.invoke()
            emitMessage("網站登入工作階段已清除")
        }
    }

    override fun onCleared() {
        CookieManager.getInstance().flush()
        popupWebView?.let(::destroyWebView)
        popupWebView = null
        webViews.values.forEach(::destroyWebView)
        webViews.clear()
        super.onCleared()
    }

    private fun createWebView(tab: WebTab?): WebView {
        val webView = WebView(appContext)
        webView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        configureSettings(webView)

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                updateState(
                    tab,
                    url = url,
                    loading = true,
                    progress = 0,
                    errorMessage = null,
                    externalHost = externalHostFor(url),
                    clearExternalHost = true,
                )
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                CookieManager.getInstance().flush()
                updateState(
                    tab,
                    url = url,
                    title = view.title.orEmpty().ifBlank { "論壇" },
                    loading = false,
                    progress = 100,
                    errorMessage = null,
                    externalHost = externalHostFor(url),
                    clearExternalHost = true,
                )
                if (tab != null && SafeUrlPolicy.canPersistInLocalHistory(url)) {
                    onPageVisited?.invoke(view.title.orEmpty(), url)
                }
            }

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest,
            ): Boolean {
                val uri = request.url
                if (uri.scheme.equals("http", ignoreCase = true) ||
                    uri.scheme.equals("https", ignoreCase = true)
                ) {
                    if (!SafeUrlPolicy.isTrustedForumHost(uri.toString())) {
                        updateState(tab, externalHost = SafeUrlPolicy.displayHost(uri.toString()))
                    }
                    return false
                }
                if (uri.scheme.equals("about", ignoreCase = true)) return false
                onExternalUri?.invoke(uri)
                return true
            }

            @Suppress("DEPRECATION")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                if (SafeUrlPolicy.isHttpUrl(url)) return false
                onExternalUri?.invoke(Uri.parse(url))
                return true
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError,
            ) {
                super.onReceivedError(view, request, error)
                if (request.isForMainFrame) {
                    updateState(
                        tab,
                        loading = false,
                        errorMessage = "網頁載入失敗，請檢查網路後重試（${error.errorCode}）",
                    )
                }
            }

            override fun onReceivedSslError(
                view: WebView,
                handler: SslErrorHandler,
                error: android.net.http.SslError,
            ) {
                handler.cancel()
                emitMessage("已拒絕不安全的 TLS 憑證，未繼續載入")
            }
        }

        webView.webChromeClient = createChromeClient(tab)
        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            if (!SafeUrlPolicy.isHttpUrl(url)) {
                emitMessage("此下載連結需要留在原網頁完成，App 未儲存不明資料")
            } else {
                requestDownload?.invoke(
                    DownloadRequest(
                        url = url,
                        userAgent = userAgent,
                        contentDisposition = contentDisposition,
                        mimeType = mimeType,
                        referer = viewUrl(webView),
                        cookie = CookieManager.getInstance().getCookie(url),
                    ),
                )
            }
        }
        return webView
    }

    @Suppress("SetJavaScriptEnabled")
    private fun configureSettings(webView: WebView) {
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = false
        settings.allowContentAccess = true
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.setSupportMultipleWindows(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.mediaPlaybackRequiresUserGesture = true
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        settings.userAgentString = "${settings.userAgentString} SehuatangAndroid/0.1"
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false)
        settings.safeBrowsingEnabled = true
    }

    private fun createChromeClient(tab: WebTab?): WebChromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            updateState(tab, progress = newProgress, loading = newProgress < 100)
        }

        override fun onReceivedTitle(view: WebView, title: String) {
            super.onReceivedTitle(view, title)
            if (tab == null) {
                _popup.value = _popup.value?.copy(title = title.ifBlank { "新視窗" })
            } else {
                updateState(tab, title = title.ifBlank { "論壇" })
            }
        }

        override fun onShowFileChooser(
            webView: WebView,
            filePathCallback: ValueCallback<Array<Uri>>,
            fileChooserParams: FileChooserParams,
        ): Boolean {
            this@WebSessionViewModel.fileChooserCallback?.onReceiveValue(null)
            this@WebSessionViewModel.fileChooserCallback = filePathCallback
            requestFilePicker?.invoke(fileChooserParams.mode == FileChooserParams.MODE_OPEN_MULTIPLE)
                ?: run {
                    this@WebSessionViewModel.fileChooserCallback?.onReceiveValue(null)
                    this@WebSessionViewModel.fileChooserCallback = null
                }
            return true
        }

        override fun onCreateWindow(
            view: WebView,
            isDialog: Boolean,
            isUserGesture: Boolean,
            resultMsg: android.os.Message,
        ): Boolean {
            if (!isUserGesture) return false
            closePopup()
            val child = createWebView(null)
            popupWebView = child
            _popup.value = PopupWindowState(child)
            val transport = resultMsg.obj as? WebView.WebViewTransport ?: run {
                closePopup()
                return false
            }
            transport.webView = child
            resultMsg.sendToTarget()
            return true
        }

        override fun onCloseWindow(window: WebView) {
            super.onCloseWindow(window)
            if (window === popupWebView) closePopup()
        }
    }

    private fun viewUrl(webView: WebView): String? = webView.url

    private fun publishStateFor(tab: WebTab, webView: WebView) {
        _state.value = _state.value.copy(
            tab = tab,
            title = webView.title.orEmpty().ifBlank { "論壇" },
            url = webView.url,
            loading = webView.progress < 100,
            progress = webView.progress,
            errorMessage = null,
            externalHost = webView.url
                ?.takeUnless(SafeUrlPolicy::isTrustedForumHost)
                ?.let(SafeUrlPolicy::displayHost),
        )
    }

    private fun updateState(
        tab: WebTab?,
        title: String? = null,
        url: String? = null,
        loading: Boolean? = null,
        progress: Int? = null,
        errorMessage: String? = null,
        externalHost: String? = null,
        clearExternalHost: Boolean = false,
    ) {
        if (tab == null || tab != activeTab) return
        val current = _state.value
        _state.value = current.copy(
            tab = tab,
            title = title ?: current.title,
            url = url ?: current.url,
            loading = loading ?: current.loading,
            progress = progress ?: current.progress,
            errorMessage = errorMessage,
            externalHost = if (clearExternalHost) externalHost else externalHost ?: current.externalHost,
        )
    }

    private fun externalHostFor(url: String?): String? =
        url?.takeUnless(SafeUrlPolicy::isTrustedForumHost)?.let(SafeUrlPolicy::displayHost)

    private fun emitMessage(message: String) {
        _events.tryEmit(WebEvent.Message(message))
    }

    private fun destroyWebView(webView: WebView) {
        (webView.parent as? ViewGroup)?.removeView(webView)
        webView.stopLoading()
        webView.webChromeClient = null
        webView.destroy()
    }
}
