package com.qbox.tv.spider

import android.content.Context
import android.util.Log
import dalvik.system.DexClassLoader
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Jar Spider 加载器。
 *
 * 饭太硬类 jar 是自包含 dex（自带 com.github.catvod.crawler.Spider 基类），
 * 宿主提供 okhttp/gson/sardine 父类加载器依赖即可。
 * 配置 api 形如 "csp_XXX"，映射到 jar 内 com.github.catvod.spider.XXX。
 */
object JarLoader {
    private const val TAG = "JarLoader"
    private const val BASE_PKG = "com.github.catvod.spider."

    private val exec = Executors.newSingleThreadExecutor()
    private val loaders = ConcurrentHashMap<String, DexClassLoader>()   // url -> loader
    private val instances = ConcurrentHashMap<String, Any>()            // siteKey -> spider instance

    fun jarDir(context: Context): File = context.getDir("jars", Context.MODE_PRIVATE)

    /** 下载 jar 到本地（md5 校验可选）。返回本地文件。 */
    fun fetchJar(context: Context, url: String, md5: String? = null): File {
        val name = url.substringAfterLast('/').takeIf { it.isNotEmpty() } ?: "spider.jar"
        val file = File(jarDir(context), name)
        if (file.exists() && (md5 == null || md5Of(file) == md5)) return file
        file.delete()
        val tmp = File(jarDir(context), "$name.part")
        val request = okhttp3.Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build()
        okhttp3.OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build().newCall(request).execute().use { resp ->
                check(resp.isSuccessful) { "jar 下载失败 HTTP ${resp.code}" }
                tmp.outputStream().use { resp.body!!.byteStream().copyTo(it) }
            }
        if (md5 != null) {
            val actual = md5Of(tmp)
            check(actual == md5) { "jar md5 不匹配: $actual != $md5" }
        }
        tmp.renameTo(file)
        loaders.remove(url)
        return file
    }

    private fun md5Of(file: File): String {
        val digest = MessageDigest.getInstance("MD5")
        file.inputStream().use { input ->
            val buf = ByteArray(8192)
            while (true) {
                val n = input.read(buf); if (n < 0) break
                digest.update(buf, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /** 为站点加载并实例化 Spider（同步，调用方负责放后台线程）。 */
    fun load(context: Context, jarUrl: String, api: String, siteKey: String): Any {
        val cacheKey = "$siteKey@$jarUrl"
        instances[cacheKey]?.let { return it }

        val md5 = jarUrl.substringAfter(";md5;", "")
            .takeIf { it.isNotEmpty() && it != jarUrl }
        val cleanUrl = jarUrl.substringBefore(";md5;").substringBefore(";")
        val jarFile = fetchJar(context, cleanUrl, md5?.takeIf { it.length == 32 })

        val loader = loaders.getOrPut(cleanUrl) {
            DexClassLoader(jarFile.absolutePath, jarDir(context).absolutePath, null, JarLoader::class.java.classLoader)
        }

        // api 可能带路径或参数：csp_XXX 或 /xxx/csp_XXX
        val simple = api.substringAfterLast("csp_").substringBefore(";").substringBefore("?")
        val className = "$BASE_PKG$simple"
        val cls = loader.loadClass(className)
        val instance = cls.getDeclaredConstructor().newInstance()
        instances[cacheKey] = instance
        Log.i(TAG, "loaded $className from ${jarFile.name}")
        return instance
    }

    /** 在单线程执行器上调用 Spider 方法，超时保护。 */
    fun call(spider: Any, method: String, timeoutSec: Long, vararg args: Any?): String {
        return exec.submit<String> {
            invoke(spider, method, args)
        }.get(timeoutSec, TimeUnit.SECONDS)
    }

    private fun invoke(spider: Any, method: String, args: Array<out Any?>): String {
        val cls = spider.javaClass
        // 按签名尝试：优先精确匹配参数类型，失败回退所有同名方法
        val candidates = cls.methods.filter { it.name == method && it.parameterTypes.size == args.size }
        val m = candidates.firstOrNull() ?: throw NoSuchMethodException("$method(${args.size} args) on ${cls.name}")
        m.isAccessible = true
        return when (val result = m.invoke(spider, *args)) {
            null -> ""
            is String -> result
            else -> result.toString()
        }
    }

    /** init 特殊处理：有 init(Context) 和 init(Context, String) 两个重载。 */
    fun init(spider: Any, context: Context, ext: String?) {
        exec.submit<Any?> {
            val cls = spider.javaClass
            val m2 = runCatching { cls.getMethod("init", Context::class.java, String::class.java) }.getOrNull()
            if (m2 != null) m2.invoke(spider, context, ext ?: "")
            else cls.getMethod("init", Context::class.java).invoke(spider, context)
            null
        }.get(15, TimeUnit.SECONDS)
    }
}
