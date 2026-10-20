package com.qbox.tv.spider

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.qbox.tv.model.CategoryResult
import com.qbox.tv.model.DetailResult
import com.qbox.tv.model.HomeResult
import com.qbox.tv.model.PlayerResult
import com.qbox.tv.model.Movie
import com.qbox.tv.config.SiteParcel

/**
 * Spider 统一运行时：加载站点 → 反射调用 → JSON 解析 → 强类型结果。
 * 所有调用同步阻塞，UI 层自行放到后台线程。
 */
object SpiderRuntime {
    private const val TAG = "SpiderRuntime"
    private val gson = Gson()

    private val loaded = HashMap<String, Any>()

    fun ensureSite(context: Context, site: SiteParcel): Any {
        synchronized(loaded) {
            loaded[site.key]?.let { return it }
            // type 0/3 + api 以 csp_ 开头 → jar
            val jarUrl = site.jar ?: contextAppSpider(context)
            val spider = JarLoader.load(context, jarUrl, site.api, site.key)
            JarLoader.init(spider, context.applicationContext, site.ext)
            loaded[site.key] = spider
            return spider
        }
    }

    /** 配置级 spider 地址（无站点级 jar 时使用）。 */
    private fun contextAppSpider(context: Context): String {
        val s = com.qbox.tv.config.ConfigRepository.current?.spiderUrl()
            ?: error("配置未包含 spider 地址")
        return s
    }

    fun home(context: Context, site: SiteParcel): HomeResult {
        val raw = JarLoader.call(ensureSite(context, site), "homeContent", 30, true)
        return gson.fromJson(raw, HomeResult::class.java)
    }

    fun category(context: Context, site: SiteParcel, tid: String, pg: Int, extend: Map<String, String>): CategoryResult {
        val raw = JarLoader.call(ensureSite(context, site), "categoryContent", 30,
            tid, pg.toString(), true, extend)
        return gson.fromJson(raw, CategoryResult::class.java)
    }

    fun detail(context: Context, site: SiteParcel, id: String): Movie? {
        val raw = JarLoader.call(ensureSite(context, site), "detailContent", 30, listOf(id))
        val result = gson.fromJson(raw, DetailResult::class.java)
        return result.list?.firstOrNull()
    }

    fun search(context: Context, site: SiteParcel, key: String): List<Movie> {
        val spider = ensureSite(context, site)
        // searchContent 有 (key,quick) 与 (key,quick,pg) 两个版本，先试三参
        val raw = runCatching {
            JarLoader.call(spider, "searchContent", 30, key, false, 1)
        }.recoverCatching {
            JarLoader.call(spider, "searchContent", 30, key, false)
        }.getOrDefault("")
        if (raw.isBlank()) return emptyList()
        return runCatching {
            gson.fromJson(raw, CategoryResult::class.java).list ?: emptyList()
        }.getOrElse { emptyList() }
    }

    fun player(context: Context, site: SiteParcel, flag: String, id: String): PlayerResult {
        val raw = JarLoader.call(ensureSite(context, site), "playerContent", 30,
            flag, id, null as List<String>?)
        val result = gson.fromJson(raw, PlayerResult::class.java)
        return result
    }
}
