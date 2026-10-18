# Sehuatang Android

Sehuatang 是色花堂的獨立第三方 Android 客戶端。它不代表、也不冒充論壇官方產品；會員權限、登入狀態、驗證碼、發帖、回覆、積分、附件下載與簽到結果都由 `https://www.sehuatang.org/` 原站決定。

本專案採用 Jetpack Compose + Material 3 作為 App 外框，並以 Android WebView 保留論壇真實流程。這個取向是刻意的：Discuz 表單的 `formhash`、驗證碼、會員權限與回覆積分不能用未確認的 API 或自製表單取代。

## 已實作

- Android 8.0（API 26）以上，Compile/Target SDK 35。
- Material 3 淺色、深色與跟隨系統主題；支援 Edge-to-Edge。
- 「首頁／版區／搜尋／訊息／我的」五區導覽。
- 論壇首頁、登入、搜尋、站內私訊、通知、會員中心、我的主題、每日簽到等原站入口。
- WebView JavaScript、DOM Storage、Cookie、頁面歷史、返回/前進/重新載入與載入失敗重試。
- 每個主要網站分頁保留獨立 WebView 實例，切換分頁時保留網站歷史與目前捲動位置；不在 Compose 重組時重建 WebView。
- `target=_blank`／`window.open` 的 App 內多視窗；可人工操作外部附件驗證頁。
- 網頁檔案上傳（Android SAF，無需開放整個檔案系統）。
- 使用系統 DownloadManager 下載 HTTP(S) 檔案，傳遞該下載 URL 對應的 Cookie、User-Agent、Referer，顯示通知與下載進度，下載完成可從 App 開啟。
- 下載完成後檢查 MIME/檔案前綴，避免把 HTML 驗證頁誤留成附件；`blob:` 或非 HTTP(S) 來源會提示使用者回到原始網站流程。
- 本機收藏與最近瀏覽（SharedPreferences + JSON）；只保留可信論壇網址，不保留登入或疑似憑證查詢參數。
- 不保存明文密碼，不輸出 Cookie/Token，不含廣告或分析 SDK。
- GitHub Actions：Push/PR 執行單元測試、Debug APK 建置並上傳 artifact。

## 尚未宣稱完成的部分

- 尚未提供獨立原生文章列表、文章解析器或原生編輯器；閱讀、發帖、回覆、引用、附件、驗證碼與私訊先透過原站 WebView 完成，以保留網站的真實權限和積分流程。
- 尚未實作背景輪詢、FCM、Web Push 或常駐 Service。前景使用時可查看網站未讀狀態，App 關閉後不宣稱即時推播。
- 尚未把網站積分數字鏡像到原生畫面；回覆是否獲得積分只以論壇自己的會員頁／紀錄為準。
- 本機沒有可用的會員帳號、真人驗證碼或官方 APK，故登入後流程、外部驗證與特定附件下載仍須在真機完成驗收。

## 開發環境與建置

需求：JDK 17（JDK 21 亦可執行）、Android SDK Platform 35、Build Tools 35.0.0。Gradle Wrapper 已提交，第一次建置會下載 Gradle 與 Maven 依賴。

```bash
./gradlew testDebugUnitTest assembleDebug
```

APK 輸出位置：

```text
app/build/outputs/apk/debug/app-debug.apk
```

本專案沒有提交 `local.properties`。若本機找不到 SDK，請在本機產生該檔案並設定 `sdk.dir`，或設定 `ANDROID_HOME`／`ANDROID_SDK_ROOT`。

## 架構

```text
app/src/main/java/com/william12233/sehuatang/
├── data/model                 本機收藏、歷史與主題設定模型
├── data/repository            本機資料儲存，不接觸網站密碼
├── ui/                        Compose 導覽、首頁、搜尋、我的、主題
└── web/
    ├── navigation              論壇網址與安全網址判斷
    ├── session                 Cookie/WebView/多視窗/檔案選擇器
    └── download                DownloadManager、進度與 HTML 防誤存
```

`WebSessionViewModel` 只在原站流程中啟用 JavaScript，沒有開放 JavaScript Interface，也沒有關閉 TLS 憑證驗證。未知 HTTP(S) 網域可以在受限 WebView 中供附件人工驗證，但不會被當作可信論壇網域，也不會被加入本機收藏/歷史。非 HTTP(S) 的 `intent:`、`file:`、`javascript:` 導覽不會被直接執行；電話與郵件連結才交給系統處理。

清除 Cookie 與網站資料的 API 已封裝在 `WebSessionViewModel.clearWebsiteSession()`；「我的 → 外觀與資料 → 清除網站登入工作階段」會在確認後同步清除 WebView Cookie、網站儲存資料、快取與歷史，但不會刪除本機收藏或 Android 下載檔案。

## 網站調查紀錄

2026-10-18 從無登入資料的開發環境以 HTTPS 讀取論壇首頁、登入、搜尋、私訊、通知、簽到、官方 App 帖與 Discuz 常見導覽路由。伺服器回傳 `static/safe/css/main.css`、`static/safe/js/web.js`、`zepto.min.js`、`mainv2.js` 的反自動化/人機驗證頁，而不是可直接解析的論壇 DOM。這是網站限制，不是 API；專案沒有嘗試繞過它，交由真實 WebView 與使用者人工完成。

未發現可安全確認的公開 REST API，也沒有從工作區取得官方 APK。因此原生層不捏造 API、不猜測表單欄位、不自行增加積分；發帖與回覆必須由使用者在網站頁面明確提交。

## 驗收清單

以下需要會員權限或真人互動的項目，在沒有帳號/真機/驗證碼前只標示為「待實機驗證」，不把自動化建置當作通過：

1. 開啟 App → 首頁 → 「開啟論壇」→ 原站首頁。
2. 「登入」→ 使用者自行填寫帳密、安全提問、驗證碼或兩步驟驗證 → 關閉 App → 重開 → 確認 Cookie 仍在。
3. 從文章內開啟圖片/附件 → 返回 → 確認 WebView 歷史與捲動位置保留。
4. 在原站選擇版區 → 開啟發表主題編輯器 → 輸入內容與附件 → 由使用者按下提交 → 重新載入確認主題存在。
5. 在文章內回覆/引用 → 由使用者按下提交 → 重新載入確認回覆存在；積分只能到論壇會員紀錄核對。
6. 點附件 → 在原站/外部驗證頁人工完成圖片、滑塊或其他驗證 → 下載 → 確認 Android `Downloads` 目錄有正確檔案且不是 HTML。
7. 「訊息」→ 私訊列表 → 對話 → 回覆；「我的」→ 通知頁。
8. 「每日簽到」→ 人工完成論壇要求的驗證 → 使用者自行按簽到，狀態以網站顯示為準。
9. 切換淺色/深色/跟隨系統、旋轉/返回手勢、切換五個分頁與重新開啟 App。

已可在無帳號條件下自動驗證的項目：

- `SafeUrlPolicyTest`：可信網域、登入/敏感網址、非 HTTP(S) 導覽。
- `DownloadFileNameTest`：控制字元、空白名稱與檔名長度限制。
- `HomeScreenTest`：Compose 首頁免登入 smoke test（需 Android instrumentation/emulator）。
- `assembleDebug`：Debug APK 編譯。

## GitHub Actions

Workflow 位於 `.github/workflows/android.yml`，在 Push/PR 執行：

```text
testDebugUnitTest → compileDebugAndroidTestKotlin → assembleDebug → upload app-debug.apk
```

不會在公開日誌輸出帳密、Cookie、Token 或其他會員資料。APK 由 Actions 的 `sehuatang-debug-apk` artifact 提供下載。
