package com.qbox.tv.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.qbox.tv.model.Movie
import com.qbox.tv.model.PlayEpisode
import com.qbox.tv.model.PlayGroup
import com.qbox.tv.model.PlayerResult
import com.qbox.tv.spider.SpiderRuntime
import kotlin.concurrent.thread

/** 点播页：左侧分类栏 + 右海报网格，滚到底自动翻页。 */
class VodActivity : Activity() {

    companion object {
        const val EXTRA_SITE = "site"
    }

    private lateinit var site: com.qbox.tv.config.SiteParcel
    private lateinit var categoryBar: RecyclerView
    private lateinit var grid: RecyclerView
    private lateinit var titleView: TextView
    private lateinit var statusView: TextView

    private val main = Handler(Looper.getMainLooper())
    private var categories: List<Pair<String, String>> = emptyList() // (tid, name)
    private var currentTid = ""
    private var page = 1
    private var total = 1
    private var loading = false
    private val movies = mutableListOf<Movie>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        site = intent.getSerializableExtra(EXTRA_SITE) as com.qbox.tv.config.SiteParcel
        setContentView(buildUi())
        thread { loadHome() }
    }

    private fun buildUi(): View {
        val dp = { v: Int -> Theme.dp(this, v) }
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Theme.BG) }

        // 顶栏
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(24), dp(16), dp(24), dp(10))
        }
        titleView = Theme.label(this, site.name, 20, Theme.TEXT, bold = true)
        top.addView(titleView)
        top.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(0, 1, 1f) })
        statusView = Theme.label(this, "", 12, Theme.TEXT_FAINT)
        top.addView(statusView)
        root.addView(top, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val body = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        root.addView(body, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        // 左侧分类栏
        categoryBar = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@VodActivity)
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        body.addView(categoryBar, LinearLayout.LayoutParams(dp(150), ViewGroup.LayoutParams.MATCH_PARENT))

        // 右侧网格
        val gridWrap = FrameLayout(this)
        grid = RecyclerView(this).apply {
            layoutManager = GridLayoutManager(this@VodActivity, 4)
            setPadding(dp(8), dp(4), dp(16), dp(24))
            clipToPadding = false
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                    val lm = rv.layoutManager as GridLayoutManager
                    if (!loading && page < total &&
                        lm.findLastVisibleItemPosition() >= movies.size - 8) {
                        loadPage(currentTid, page + 1)
                    }
                }
            })
        }
        gridWrap.addView(grid, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        body.addView(gridWrap, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        return root
    }

    private fun loadHome() {
        setStatus("加载分类…")
        runCatching {
            val home = SpiderRuntime.home(this, site)
            val cats = home.class_.orEmpty().map { it.id() to it.name() }.filter { it.first != "0" }
            val firstPage = if (cats.isNotEmpty())
                SpiderRuntime.category(this, site, cats.first().first, 1, emptyMap()) else null
            Triple(cats, firstPage?.list.orEmpty(), firstPage?.pagecount ?: 1)
        }.onSuccess { (cats, list, pc) ->
            main.post {
                categories = cats
                currentTid = cats.firstOrNull()?.first ?: ""
                total = pc
                movies.clear(); movies.addAll(list)
                categoryBar.adapter = CategoryAdapter(this, categories, currentTid) { tid ->
                    currentTid = tid; page = 1; total = 1; movies.clear()
                    grid.adapter?.notifyDataSetChanged()
                    categoryBar.adapter?.notifyDataSetChanged()
                    loadPage(tid, 1)
                }
                grid.adapter = MovieAdapter(this, movies) { movie -> openDetail(movie) }
                setStatus("")
                if (movies.isEmpty()) loadPage(currentTid, 1)
            }
        }.onFailure {
            main.post { setStatus("加载失败：${it.message?.take(40)}") }
        }
    }

    private fun loadPage(tid: String, pg: Int) {
        if (tid.isBlank() || loading) return
        loading = true
        setStatus("第 $pg 页…")
        thread {
            runCatching {
                SpiderRuntime.category(this, site, tid, pg, emptyMap())
            }.onSuccess { result ->
                main.post {
                    loading = false
                    page = pg
                    total = result.pagecount.takeIf { it > 0 } ?: result.total.takeIf { it > 0 } ?: 1
                    val start = movies.size
                    movies.addAll(result.list.orEmpty())
                    if (pg == 1) movies.removeAll { it.id().isBlank() }
                    grid.adapter?.notifyDataSetChanged()
                    setStatus(if (page < total) "" else "到底了")
                }
            }.onFailure {
                main.post { loading = false; setStatus("翻页失败") }
            }
        }
    }

    private fun openDetail(movie: Movie) {
        val intent = Intent(this, VideoActivity::class.java)
        intent.putExtra(VideoActivity.EXTRA_SITE, site)
        intent.putExtra(VideoActivity.EXTRA_MOVIE_ID, movie.id())
        intent.putExtra(VideoActivity.EXTRA_MOVIE_NAME, movie.name())
        intent.putExtra(VideoActivity.EXTRA_MOVIE_PIC, movie.vod_pic)
        startActivity(intent)
    }

    private fun setStatus(text: String) {
        statusView.text = text
    }
}

/** 分类栏适配器 */
class CategoryAdapter(
    private val ctx: android.content.Context,
    private val items: List<Pair<String, String>>,
    private var selected: String,
    private val onSelect: (String) -> Unit,
) : RecyclerView.Adapter<CategoryAdapter.Holder>() {

    class Holder(val view: TextView) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val dp = { v: Int -> Theme.dp(ctx, v) }
        val view = TextView(ctx).apply {
            textSize = 14f
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(8), dp(12))
        }
        return Holder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val (tid, name) = items[position]
        val isSel = tid == selected
        holder.view.text = name
        holder.view.setTextColor(if (isSel) Theme.TEXT else Theme.TEXT_DIM)
        holder.view.background = if (isSel)
            Theme.corner(ctx, Theme.SURFACE_HI, Theme.RADIUS) else null
        holder.view.setOnClickListener {
            val old = selected
            selected = tid
            notifyItemChanged(items.indexOfFirst { it.first == old })
            notifyItemChanged(position)
            onSelect(tid)
        }
    }
}

/** 海报网格适配器 */
class MovieAdapter(
    private val ctx: android.content.Context,
    private val movies: List<Movie>,
    private val onClick: (Movie) -> Unit,
) : RecyclerView.Adapter<MovieAdapter.Holder>() {

    class Holder(val view: PosterCard) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val dp = { v: Int -> Theme.dp(ctx, v) }
        val card = PosterCard(ctx)
        card.layoutParams = RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(dp(4), dp(4), dp(4), dp(10)) }
        return Holder(card)
    }

    override fun getItemCount(): Int = movies.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val movie = movies[position]
        holder.view.bind(movie.name(), movie.vod_pic, movie.vod_remarks)
        holder.view.setOnClickListener { onClick(movie) }
    }
}
