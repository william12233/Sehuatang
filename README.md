# Sehuatang Android 原生論壇客戶端

這是使用 Kotlin、Jetpack Compose、Material 3 製作的獨立第三方 Android 論壇客戶端。色花堂只作為遠端資料與服務來源；首頁、版區、文章列表、文章閱讀、搜尋、訊息與「我的」都由 App 自己的 Compose UI 繪製，不把原始網站當成內容畫面，也不顯示網站 Logo、廣告、手機版導覽列或瀏覽器工具列。

## 目前架構

```text
Compose Screens
      ↓
ViewModel + StateFlow
      ↓
ForumRepository
      ↓
ForumDataSource / SessionCookieJar
      ↓
OkHttp + Jsoup
      ↓
sehuatang.org
```

- `data/model/ForumModels.kt`：版區、文章、帖子、結構化正文區塊、附件、登入/發文表單模型。
- `data/remote/ForumHttpClient.kt`：OkHttp、Cookie 工作階段、表單與 multipart 請求。
- `data/remote/ForumParsers.kt`：`ForumIndexParser`、`ForumListParser`、`ThreadListParser`、`ThreadDetailParser`、`SearchParser`、`LoginParser`、`PostFormParser`、`MessageParser`。
- `data/repository/ForumRepository.kt`：ViewModel 與遠端資料來源的邊界。
- `ui/ForumViewModel.kt`：載入、錯誤、站點驗證、搜尋、登入、發文與回覆狀態。
- `ui/**`：原生首頁、版區、文章列表、文章閱讀、搜尋、登入、編輯器、訊息、我的與隔離驗證畫面。
- `LocalDataRepository`：DataStore 保存收藏、閱讀歷史與外觀設定；不保存明文密碼。

正文會被解析成 `Text`、`Image`、`Link`、`Quote`、`Code`、`AttachmentBlock`、`Video` 或 `Unsupported`，Compose 不接收 HTML。圖片使用 Coil，文章與帖子列表使用 LazyColumn，長文與圖片不會先轉成網頁或截圖。

## 網站調查與限制

已用 HTTPS 實測以下公开路由：首頁/`forum.php`、版區、文章與搜尋。2026-10-18 的未登入回應是站點反自動化驗證頁，包含 `static/safe/css/main.css`、`static/safe/js/web.js`、`mainv2.js` 與 `safeid`，不是論壇 DOM；目前沒有確認到可安全使用的 JSON、Discuz Mobile API 或 Ajax JSON 介面。App 因此不捏造 API，也不把驗證頁誤當文章資料。

收到驗證頁時，原生 UI 會顯示明確錯誤與「開啟隔離驗證」入口。WebView 僅在該入口中供使用者本人完成站點/第三方附件驗證；完成後 Cookie 回到原生 HTTP 工作階段。一般閱讀、搜尋、訊息與會員頁不使用 WebView。

目前使用的 Parser 以 Discuz HTML 的實際節點命名與相對網址處理為基礎，但在本環境只能直接實測驗證頁，尚未取得登入後的論壇 DOM。因此登入後版區、文章、發文表單與附件流程仍須真人帳號與站點驗證實測；Parser 不會以測試資料代替正式論壇內容。

## 原生功能

- 首頁：版區、最新/熱門文章、最近閱讀，資料來自 Repository。
- 版區：原生版區卡片與原生文章列表。
- 文章：標題、作者、時間、帖子、文字、圖片、連結、引用、程式碼、附件、分頁與原生回覆入口；圖片可全螢幕手勢縮放。
- 搜尋：先解析論壇提供的搜尋表單，再依實際 action、method、欄位送出，結果以原生卡片顯示。
- 登入：Compose 表單從論壇實際登入 HTML 取得欄位與 hidden token；不把密碼寫入本機。
- 發文/回覆：Compose 編輯器從實際表單取得 action、method、hidden 欄位、formhash、正文/標題/附件欄位，支援 multipart 附件；只有伺服器回應可確認成功時才顯示成功。
- 附件：原生附件卡片、必要時隔離驗證、Android DownloadManager、Cookie/Referer/MIME/HTML 誤下載檢查。
- 簽到：原生簽到畫面會先解析論壇實際表單，只提交伺服器回傳的欄位，並要求使用者明確點擊；未確認成功文字時不會報成功。
- 私訊：原生列表與未讀狀態模型；背景常駐輪詢與推播尚未加入。
- 外觀：Material 3、繁體中文、淺/深色、系統動態色可選、單一底部導覽列：首頁／版區／搜尋／訊息／我的。

## 測試與建置

需要 JDK 17、Android SDK Platform 35、Build Tools 35.0.0。

```bash
./gradlew testDebugUnitTest compileDebugAndroidTestKotlin assembleDebug
```

Debug APK：`app/build/outputs/apk/debug/app-debug.apk`

測試包含：

- 站點實測驗證頁 fixture：確認不會當作論壇內容解析。
- Discuz 結構 parser：相對網址、HTML Entity、帖子、圖片、連結、引用、附件、登入/回覆表單欄位。
- URL 安全策略與下載檔名。
- Compose 首頁使用 Repository 模型渲染文章。

本次工作區的驗證結果：`testDebugUnitTest`、`compileDebugAndroidTestKotlin`、`assembleDebug` 通過，已產生可安裝 Debug APK。沒有可用 Android Emulator，因此尚未執行 instrumentation 或擷取真機畫面。

## GitHub Actions

`.github/workflows/android.yml` 會在 Push/PR 上執行單元測試、Android test 編譯與 Debug APK 建置，並上傳 `sehuatang-debug-apk` artifact。CI 使用 JDK 17 與 Android SDK 35；不需要把本機 `local.properties` 或任何帳號資料提交到 Git。

## 尚待真人驗證

1. 站點驗證完成後取得真正論壇首頁/版區/文章 DOM，核對每個 selector 與分頁。
2. 使用者在 App 內自行登入，確認 Cookie、CSRF/formhash、安全提問與保持登入。
3. 在測試版區驗證回覆/發文、附件 multipart、權限錯誤、積分與伺服器成功回應。
4. 真人完成第三方附件驗證後，確認短效下載網址、Content-Disposition 與 Android 檔案內容。
5. 登入後私訊對話詳情、通知、收藏/簽到表單與 Samsung Galaxy S23 Ultra/不同螢幕比例的實機驗收。

本專案不會自動使用帳號發表文章、回覆、簽到或其他論壇操作，也不會繞過 CAPTCHA、人機驗證或下載限制。
