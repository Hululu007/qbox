package com.qbox.tv.model

/** TVBox 内容 JSON 结果模型。 */

data class ClassInfo(
    val type_id: Any? = null,
    val type_name: String? = null,
) {
    fun id(): String = type_id?.toString() ?: "0"
    fun name(): String = type_name ?: ""
}

data class FilterValue(val n: String? = null, val v: String? = null) {
    fun value(): String = v ?: n ?: ""
    fun label(): String = n ?: v ?: ""
}

data class Filter(
    val key: String? = null,
    val name: String? = null,
    val value: List<FilterValue>? = null,
) {
    fun id(): String = key ?: name ?: ""
}

data class Movie(
    val vod_id: Any? = null,
    val type_id: Any? = null,
    val type_name: String? = null,
    val vod_name: String? = null,
    val vod_pic: String? = null,
    val vod_remarks: String? = null,
    val vod_year: String? = null,
    val vod_area: String? = null,
    val vod_content: String? = null,
    val vod_actor: String? = null,
    val vod_director: String? = null,
    val vod_class: String? = null,
    // detail 才有
    val vod_play_from: String? = null,
    val vod_play_url: String? = null,
    val vod_tag: String? = null,
) {
    fun id(): String = vod_id?.toString() ?: ""
    fun name(): String = vod_name ?: ""
}

/** homeContent */
data class HomeResult(
    val class_: List<ClassInfo>? = null,
    val filters: Map<String, List<Filter>>? = null,
    val list: List<Movie>? = null,
)

/** categoryContent / searchContent 通用分页 */
data class CategoryResult(
    val list: List<Movie>? = null,
    val total: Int = 0,
    val page: Int = 0,
    val pagecount: Int = 0,
    val limit: Int = 0,
)

/** detailContent */
data class DetailResult(
    val list: List<Movie>? = null,
)

/** playerContent */
data class PlayerResult(
    val parse: Int = 0,
    val playUrl: String? = null,
    val url: String? = null,
    val header: Any? = null,
    val flag: String? = null,
    val danmaku: String? = null,
    val jump: String? = null,
) {
    fun realUrl(): String = url ?: playUrl ?: ""
}

/** vod_play_url 解析结果 */
data class PlayGroup(
    val name: String,
    val episodes: List<PlayEpisode>,
)

data class PlayEpisode(
    val name: String,
    val url: String,
)
