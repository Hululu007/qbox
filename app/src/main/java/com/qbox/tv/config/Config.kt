package com.qbox.tv.config

/** TVBox 配置协议数据模型（Gson 直接反序列化，字段宽容可空）。 */
data class Site(
    val key: String? = null,
    val name: String? = null,
    val type: Int = 0,
    val api: String? = null,
    val searchable: Int = 0,
    val quickSearch: Int = 0,
    val filterable: Int = 0,
    val ext: Any? = null,          // 可能是字符串/对象/数组
    val header: Any? = null,
    val timeout: Int = 0,
    val playerUrl: Any? = null,
    val jar: String? = null,       // 站点级 jar 覆盖
    val categories: List<String>? = null,
    val click: String? = null,
    val playUrl: String? = null,
    val indexs: String? = null,
)

data class Live(
    val name: String? = null,
    val type: Int = 0,
    val url: Any? = null,          // 可能是字符串/数组
    val epg: String? = null,
    val playerType: Int = 0,
)

data class Parse(
    val name: String? = null,
    val type: Int = 0,
    val url: String? = null,
    val ext: Any? = null,
    val header: Any? = null,
)

data class Config(
    var spider: String? = null,            // jar/js 地址，支持 xxx;md5;hash 校验
    val sites: List<Site>? = null,
    val lives: List<Live>? = null,
    val parses: List<Parse>? = null,
    val wallpaper: String? = null,
    val logo: String? = null,
    val flags: List<String>? = null,
    val danmu: Boolean? = null,
) {
    /** spider 字段形如 url;md5;hash，取第一段。 */
    fun spiderUrl(): String? = spider?.split(";")?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }
}
