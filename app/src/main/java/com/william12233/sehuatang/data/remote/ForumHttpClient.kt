package com.william12233.sehuatang.data.remote

import android.content.Context
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ForumHttpResponse(
    val requestedUrl: String,
    val finalUrl: String,
    val code: Int,
    val body: String,
    val contentType: String?,
)

/**
 * 論壇 HTTP 工作階段。Cookie 只保存 Cookie 本身，不保存帳號或明文密碼，也不輸出到 log。
 */
class SessionCookieJar(context: Context) : CookieJar {
    private val preferences = context.applicationContext.getSharedPreferences(
        "sehuatang_http_session",
        Context.MODE_PRIVATE,
    )
    private val cookies = mutableListOf<Cookie>()

    init {
        load()
    }

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val now = System.currentTimeMillis()
        this.cookies.removeAll { it.expiresAt < now }
        cookies.forEach { incoming ->
            this.cookies.removeAll { old ->
                old.name == incoming.name &&
                    old.domain == incoming.domain &&
                    old.path == incoming.path
            }
            if (incoming.expiresAt >= now) this.cookies += incoming
        }
        persist()
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        cookies.removeAll { it.expiresAt < now }
        return cookies.filter { it.matches(url) }
    }

    @Synchronized
    fun cookieHeader(url: String): String? {
        val parsed = url.toHttpUrlOrNull() ?: return null
        return loadForRequest(parsed)
            .joinToString("; ") { "${it.name}=${it.value}" }
            .ifBlank { null }
    }

    /** 將隔離驗證 WebView 的同網域 Cookie 交回原生 HTTP 工作階段。 */
    @Synchronized
    fun importCookieHeader(url: String, cookieHeader: String?) {
        val parsed = url.toHttpUrlOrNull() ?: return
        cookieHeader
            ?.split(';')
            ?.map(String::trim)
            ?.filter { it.contains('=') }
            ?.forEach { pair ->
                Cookie.parse(parsed, pair)?.let { cookie ->
                    saveFromResponse(parsed, listOf(cookie))
                }
            }
    }

    @Synchronized
    fun clear() {
        cookies.clear()
        preferences.edit().remove(KEY_COOKIES).apply()
    }

    private fun load() {
        runCatching {
            val array = JSONArray(preferences.getString(KEY_COOKIES, "[]"))
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val builder = Cookie.Builder()
                    .name(item.optString("name"))
                    .value(item.optString("value"))
                    .path(item.optString("path", "/"))
                val domain = item.optString("domain")
                if (item.optBoolean("hostOnly", false)) builder.hostOnlyDomain(domain)
                else builder.domain(domain)
                val expiresAt = item.optLong("expiresAt", Long.MAX_VALUE)
                if (expiresAt < Long.MAX_VALUE) builder.expiresAt(expiresAt)
                if (item.optBoolean("secure")) builder.secure()
                if (item.optBoolean("httpOnly")) builder.httpOnly()
                builder.build().let(cookies::add)
            }
        }
    }

    private fun persist() {
        val array = JSONArray()
        cookies.forEach { cookie ->
            array.put(
                JSONObject()
                    .put("name", cookie.name)
                    .put("value", cookie.value)
                    .put("domain", cookie.domain)
                    .put("path", cookie.path)
                    .put("expiresAt", cookie.expiresAt)
                    .put("hostOnly", cookie.hostOnly)
                    .put("secure", cookie.secure)
                    .put("httpOnly", cookie.httpOnly),
            )
        }
        preferences.edit().putString(KEY_COOKIES, array.toString()).apply()
    }

    private companion object {
        const val KEY_COOKIES = "cookies"
    }
}

/** 封裝所有論壇 HTTP 請求，讓 Parser 與 Compose 都不直接接觸 OkHttp。 */
class ForumHttpClient(context: Context) {
    val session = SessionCookieJar(context)

    private val client = OkHttpClient.Builder()
        .cookieJar(session)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    suspend fun get(url: String, referer: String? = null): ForumHttpResponse =
        execute(
            Request.Builder()
                .url(url)
                .get()
                .acceptHtml()
                .referer(referer)
                .build(),
        )

    suspend fun postForm(
        url: String,
        fields: Map<String, String>,
        referer: String? = null,
    ): ForumHttpResponse {
        val body = FormBody.Builder().apply {
            fields.forEach { (name, value) -> add(name, value) }
        }.build()
        return execute(
            Request.Builder()
                .url(url)
                .post(body)
                .acceptHtml()
                .referer(referer)
                .build(),
        )
    }

    suspend fun postMultipart(
        url: String,
        fields: Map<String, String>,
        files: List<UploadPart> = emptyList(),
        referer: String? = null,
    ): ForumHttpResponse {
        val body = MultipartBody.Builder().setType(MultipartBody.FORM).apply {
            fields.forEach { (name, value) -> addFormDataPart(name, value) }
            files.forEach { file ->
                addFormDataPart(
                    file.fieldName,
                    file.fileName,
                    file.body,
                )
            }
        }.build()
        return execute(
            Request.Builder()
                .url(url)
                .post(body)
                .acceptHtml()
                .referer(referer)
                .build(),
        )
    }

    fun cookieHeader(url: String): String? = session.cookieHeader(url)

    fun clearSession() = session.clear()

    private suspend fun execute(request: Request): ForumHttpResponse {
        // The suspend boundary lives above this client; OkHttp executes synchronously on Dispatchers.IO.
        val response = client.newCall(request).execute()
        return response.toForumResponse()
    }

    private fun Response.toForumResponse(): ForumHttpResponse {
        val originalUrl = request.url.toString()
        val responseBody = body ?: "".toResponseBody("text/plain".toMediaType())
        responseBody.use { body ->
            return ForumHttpResponse(
                requestedUrl = originalUrl,
                finalUrl = request.url.toString(),
                code = code,
                body = body.string(),
                contentType = body.contentType()?.toString(),
            )
        }
    }

    private fun Request.Builder.acceptHtml(): Request.Builder =
        header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "zh-TW,zh;q=0.9,en;q=0.6")
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 " +
                    "Chrome/126.0 Mobile Safari/537.36 SehuatangAndroid/1.0",
            )

    private fun Request.Builder.referer(value: String?): Request.Builder = apply {
        if (!value.isNullOrBlank()) header("Referer", value)
    }

    data class UploadPart(
        val fieldName: String,
        val fileName: String,
        val body: RequestBody,
    )
}
