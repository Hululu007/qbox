package com.qbox.tv.model

/** 解析 vod_play_url：格式 "线路1$集1$u1#集2$u2$$$线路2$..." */
object PlayParser {

    fun parse(from: String?, url: String?): List<PlayGroup> {
        if (url.isNullOrBlank()) return emptyList()
        val names = (from ?: "").split("$$$").map { it.trim() }
        val groups = url.split("$$$")
        return groups.mapIndexed { i, group ->
            val name = names.getOrNull(i)?.takeIf { it.isNotBlank() } ?: "线路${i + 1}"
            val episodes = group.split("#").mapNotNull { ep ->
                val parts = ep.split("$")
                when {
                    parts.size >= 2 -> PlayEpisode(parts[0].trim(), parts.subList(1, parts.size).joinToString("$").trim())
                    parts.size == 1 && parts[0].contains("://") -> PlayEpisode("第${i + 1}集", parts[0].trim())
                    else -> null
                }
            }.filter { it.url.isNotBlank() }
            PlayGroup(name, episodes)
        }.filter { it.episodes.isNotEmpty() }
    }
}
