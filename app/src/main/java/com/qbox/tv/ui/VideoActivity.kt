package com.qbox.tv.ui

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.qbox.tv.model.Movie
import com.qbox.tv.model.PlayGroup
import com.qbox.tv.model.PlayEpisode
import com.qbox.tv.player.QPlayer
import com.qbox.tv.spider.SpiderRuntime
import coil.load
import kotlin.concurrent.thread

/**
 * 详情页 + 内嵌播放器（OKTV 同路线）。
 * 停靠态：左海报信息 + 右选集；OK 播放 → 全屏播放。
 */
class VideoActivity : Activity() {

    companion object {
        const val EXTRA_SITE = "site"
        const val EXTRA_MOVIE_ID = "movieId"
        const val EXTRA_MOVIE_NAME = "movieName"
        const val EXTRA_MOVIE_PIC = "moviePic"
    }

    private lateinit var site: com.qbox.tv.config.SiteParcel
    private lateinit var titleView: TextView
    private lateinit var metaView: TextView
    private lateinit var introView: TextView
    private lateinit var playBtn: TextView
    private lateinit var episodesBar: RecyclerView
    private lateinit var playerView: PlayerView

    private var player: QPlayer? = null
    private var detail: Movie? = null
    private var groups: List<PlayGroup> = emptyList()
    private var currentGroup = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        site = intent.getSerializableExtra(EXTRA_SITE) as com.qbox.tv.config.SiteParcel
        val movieId = intent.getStringExtra(EXTRA_MOVIE_ID) ?: ""
        setContentView(buildUi())
        thread { loadDetail(movieId) }
    }

    private fun buildUi(): View {
        val dp = { v: Int -> Theme.dp(this, v) }
        val root = FrameLayout(this).apply { setBackgroundColor(Theme.BG) }

        // 详情层
        val info = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        root.addView(info, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        // 左侧：海报 + 信息
        val left = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(16), dp(16), dp(16))
        }
        info.addView(left, LinearLayout.LayoutParams(dp(220), ViewGroup.LayoutParams.WRAP_CONTENT))

        val poster = android.widget.ImageView(this).apply {
            scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(Theme.SURFACE)
        }
        poster.load(intent.getStringExtra(EXTRA_MOVIE_PIC)) { crossfade(true) }
        left.addView(poster, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(300)).apply { topMargin = dp(0) })

        titleView = Theme.label(this, intent.getStringExtra(EXTRA_MOVIE_NAME) ?: "", 18, Theme.TEXT, bold = true)
        left.addView(titleView, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(10) })

        metaView = Theme.label(this, "", 12, Theme.TEXT_DIM)
        left.addView(metaView, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(4) })

        playBtn = TextView(this).apply {
            text = "▶  播放"
            textSize = 15f
            setTextColor(Theme.TEXT)
            gravity = Gravity.CENTER
            isFocusable = true
            background = Theme.corner(this@VideoActivity, Theme.SURFACE_HI, 22f)
        }
        Theme.bindFocus(playBtn, radiusDp = 22f)
        left.addView(playBtn, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(44)).apply { topMargin = dp(14) })
        playBtn.setOnClickListener { playFirst() }

        // 右侧：简介 + 选集
        val right = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(16), dp(24), dp(16))
        }
        info.addView(right, LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))

        introView = TextView(this).apply {
            textSize = 13f
            setTextColor(Theme.TEXT_DIM)
            maxLines = 6
            setLineSpacing(dp(5).toFloat(), 1f)
        }
        right.addView(introView, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        right.addView(Theme.label(this, "选集", 15, Theme.TEXT, bold = true), LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(14) })

        episodesBar = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@VideoActivity, RecyclerView.VERTICAL, false)
        }
        right.addView(episodesBar, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT).apply { topMargin = dp(8) })

        // 播放层（全屏覆盖，初始隐藏）
        playerView = PlayerView(this).apply {
            useController = true
            visibility = View.GONE
        }
        root.addView(playerView, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        return root
    }

    private fun loadDetail(movieId: String) {
        runCatching {
            val movie = SpiderRuntime.detail(this, site, movieId) ?: error("详情为空")
            movie to parsePlayUrl(movie.vod_play_from ?: "", movie.vod_play_url ?: "")
        }.onSuccess { (movie, gs) ->
            Handler(Looper.getMainLooper()).post {
                detail = movie
                groups = gs
                titleView.text = movie.name()
                metaView.text = listOfNotNull(movie.vod_year, movie.vod_area, movie.vod_class)
                    .joinToString(" · ")
                introView.text = movie.vod_content?.trim() ?: "暂无简介"
                episodesBar.adapter = EpisodeAdapter(this, gs) { gi, ei -> play(gi, ei) }
            }
        }.onFailure { e ->
            Handler(Looper.getMainLooper()).post {
                introView.text = "详情加载失败：${e.message ?: ""}"
            }
        }
    }

    /** "线路1$...#...$$$线路2$..." 解析 */
    private fun parsePlayUrl(from: String, url: String): List<PlayGroup> {
        val names = from.split("\\$\\$\\$".toRegex()).map { it.trim() }
        val blocks = url.split("\\$\\$\\$".toRegex())
        return blocks.mapIndexed { i, block ->
            val name = names.getOrNull(i) ?: "线路${i + 1}"
            val episodes = block.split("#").mapNotNull { seg ->
                val parts = seg.split("$")
                if (parts.size >= 2) PlayEpisode(parts[0].trim(), parts[1].trim())
                else if (parts.size == 1 && parts[0].startsWith("http")) PlayEpisode("P", parts[0])
                else null
            }
            PlayGroup(name, episodes)
        }.filter { it.episodes.isNotEmpty() }
    }

    private fun playFirst() {
        if (groups.isEmpty()) return
        play(0, 0)
    }

    private fun play(groupIndex: Int, episodeIndex: Int) {
        val group = groups.getOrNull(groupIndex) ?: return
        val episode = group.episodes.getOrNull(episodeIndex) ?: return
        thread {
            runCatching {
                SpiderRuntime.player(this, site, group.name, episode.url)
            }.onSuccess { result ->
                val direct = result.realUrl()
                Handler(Looper.getMainLooper()).post {
                    if (direct.isNotBlank()) startPlayback(direct, result)
                    else introView.text = "该集无直链（parse 类源暂不支持，M2）"
                }
            }.onFailure { e ->
                Handler(Looper.getMainLooper()).post {
                    introView.text = "取播放地址失败：${e.message ?: ""}"
                }
            }
        }
    }

    private fun startPlayback(url: String, result: com.qbox.tv.model.PlayerResult) {
        val q = player ?: QPlayer(this).also {
            player = it
            playerView.player = it.player
        }
        val headers = (result.header as? Map<*, *>)?.mapNotNull { (k, v) ->
            (k as? String)?.let { key -> key to (v?.toString() ?: "") }
        }?.toMap()
        playerView.visibility = View.VISIBLE
        playerView.requestFocus()
        q.play(url, headers)
    }

    override fun onBackPressed() {
        if (playerView.visibility == View.VISIBLE) {
            playerView.visibility = View.GONE
            player?.player?.stop()
            player?.player?.clearMediaItems()
        } else super.onBackPressed()
    }

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }
}

/** 选集列表 */
class EpisodeAdapter(
    private val ctx: android.content.Context,
    private val groups: List<PlayGroup>,
    private val onPlay: (Int, Int) -> Unit,
) : RecyclerView.Adapter<EpisodeAdapter.GroupHolder>() {

    abstract class GroupHolder(view: View) : RecyclerView.ViewHolder(view)
    class GroupTitleHolder(val view: TextView) : GroupHolder(view)
    class EpisodeHolder(val view: TextView) : GroupHolder(view)

    private val rows: List<Row> = groups.flatMapIndexed { gi, g ->
        listOf(Row.Title(g.name)) + g.episodes.mapIndexed { ei, e -> Row.Episode(gi, ei, e.name) }
    }

    sealed class Row {
        data class Title(val name: String) : Row()
        data class Episode(val group: Int, val index: Int, val name: String) : Row()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupHolder {
        val dp = { v: Int -> Theme.dp(ctx, v) }
        val view = TextView(ctx).apply {
            textSize = 13f
            setPadding(dp(12), dp(8), dp(12), dp(8))
            isFocusable = true
        }
        return if (viewType == 0) GroupTitleHolder(view.apply { setTextColor(Theme.TEXT_DIM) })
        else EpisodeHolder(view.apply {
            setTextColor(Theme.TEXT)
            background = Theme.corner(ctx, Theme.SURFACE, Theme.RADIUS)
        })
    }

    override fun getItemViewType(position: Int): Int = if (rows[position] is Row.Title) 0 else 1

    override fun getItemCount(): Int = rows.size

    override fun onBindViewHolder(holder: GroupHolder, position: Int) {
        when (val row = rows[position]) {
            is Row.Title -> (holder as GroupTitleHolder).view.text = row.name
            is Row.Episode -> {
                val view = (holder as EpisodeHolder).view
                view.text = row.name
                Theme.bindFocus(view, radiusDp = 6f, scale = 1.0f)
                view.setOnClickListener { onPlay(row.group, row.index) }
            }
        }
    }
}
