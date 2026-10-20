package com.qbox.tv.config

import android.content.Context
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParser
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream
import java.util.zip.InflaterInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * 配置加载器：下载 → 解压 → 解析。
 * 支持：纯 JSON、GZIP/Deflate 压缩 JSON、Base64(GZIP(JSON))。
 * spider 等相对路径（./ 或不带协议）相对于配置 URL 解析。
 */
object ConfigRepository {
    private const val TAG = "ConfigRepo"
    private val gson = Gson()
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /** 当前生效配置 */
    @Volatile
    var current: Config? = null
        private set

    @Volatile
    var currentUrl: String? = null
        private set

    /** 解析相对路径为绝对路径。 */
    fun resolve(baseUrl: String?, path: String): String {
        if (path.isEmpty()) return path
        if (path.startsWith("http://") || path.startsWith("https://") || path.startsWith("file://")) return path
        val base = baseUrl ?: return path
        return try {
            java.net.URL(java.net.URL(base), path).toString()
        } catch (e: Exception) {
            path
        }
    }

    fun load(url: String): Config {
        val text = fetch(url)
        val config = parse(text) ?: error("配置解析失败")
        // 相对 spider 转绝对
        config.spiderUrl()?.let { raw ->
            val abs = resolve(url, raw)
            if (abs != raw) {
                val md5Part = config.spider!!.substringAfter(raw, "")
                config.spider = abs + md5Part
            }
        }
        current = config
        currentUrl = url
        return config
    }

    private fun fetch(url: String): String {
        val request = Request.Builder().url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 11) AppleWebKit/537.36")
            .build()
        http.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code}")
            val bytes = resp.body?.bytes() ?: error("空响应")
            return decode(bytes)
        }
    }

    /** 尝试多种解码路径。 */
    private fun decode(bytes: ByteArray): String {
        // 1. Base64 → zip/gzip/json
        val maybeBase64 = runCatching {
            val cleaned = String(bytes, Charsets.UTF_8).trim()
            if (cleaned.length > 40 && cleaned.none { it == '{' }) {
                Base64.decode(cleaned, Base64.DEFAULT)
            } else null
        }.getOrNull()
        if (maybeBase64 != null) {
            decodeCompressed(maybeBase64)?.let { return it }
        }
        // 2. 直接压缩
        decodeCompressed(bytes)?.let { return it }
        // 3. 纯文本 JSON
        return String(bytes, Charsets.UTF_8)
    }

    private fun decodeCompressed(bytes: ByteArray): String? = runCatching {
        if (bytes.size > 2 && bytes[0] == 0x1f.toByte() && bytes[1] == 0x8b.toByte()) {
            GZIPInputStream(ByteArrayInputStream(bytes)).use { return@runCatching it.readBytes().toString(Charsets.UTF_8) }
        }
        if (bytes.size > 2 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4b.toByte()) {
            // zip: 取第一个 entry
            java.util.zip.ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val out = ByteArrayOutputStream()
                        zip.copyTo(out)
                        return@runCatching out.toString("UTF-8")
                    }
                    entry = zip.nextEntry
                }
            }
        }
        InflaterInputStream(ByteArrayInputStream(bytes)).use {
            return@runCatching it.readBytes().toString(Charsets.UTF_8)
        }
        null
    }.getOrNull()

    fun parse(text: String): Config? = runCatching {
        val trimmed = text.trim()
        // 有些配置外层包了一层数组或 text 字段
        val element = JsonParser.parseString(trimmed)
        val obj = when {
            element.isJsonObject -> element.asJsonObject
            element.isJsonArray && element.asJsonArray.size() > 0 -> element.asJsonArray[0].asJsonObject
            else -> error("未知格式")
        }
        gson.fromJson(obj, Config::class.java)
    }.onFailure { Log.w(TAG, "parse fail: $it") }.getOrNull()
}
