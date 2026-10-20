package com.qbox.tv.config

/** 站点在 Activity 间传递的最小载体。 */
data class SiteParcel(
    val key: String,
    val name: String,
    val type: Int,
    val api: String,
    val ext: String?,
    val jar: String?,
    val configUrl: String,
) : java.io.Serializable {
    companion object {
        fun from(site: Site, configUrl: String) = SiteParcel(
            key = site.key ?: "",
            name = site.name ?: site.key ?: "",
            type = site.type,
            api = site.api ?: "",
            ext = site.ext as? String,
            jar = site.jar,
            configUrl = configUrl,
        )
    }
}
