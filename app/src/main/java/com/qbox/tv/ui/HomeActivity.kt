package com.qbox.tv.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.qbox.tv.config.Config
import com.qbox.tv.config.ConfigRepository
import com.qbox.tv.config.Site
import com.qbox.tv.config.SiteParcel
import kotlin.concurrent.thread

/** 首页：Oktv 式空壳——无配置显示导入界面，有配置加载站点网格。菜单键 = 更改配置。 */
class HomeActivity : Activity() {

    companion object {
        // 导入界面预填的示例地址，用户可自行替换
        const val SAMPLE_CONFIG =
            "https://raw.githubusercontent.com/gaotianliuyun/gao/main/0826.json"
        const val EXTRA_SITE = "site"
        const val PREFS = "qbox"
        const val KEY_CONFIG_URL = "configUrl"
    }

    private lateinit var statusView: TextView
    private lateinit var grid: RecyclerView
    private lateinit var prefs: SharedPreferences
    private lateinit var emptyView: View
    private val main = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        setContentView(buildUi())
        val saved = prefs.getString(KEY_CONFIG_URL, null)
        if (saved != null) loadConfig(saved) else showEmpty()
    }

    /** 菜单键：随时更换配置地址。 */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU) { promptConfigUrl(); return true }
        return super.onKeyDown(keyCode, event)
    }

    private fun showEmpty() {
        statusView.text = "未导入配置"
        emptyView.visibility = View.VISIBLE
    }

    private fun importConfig(url: String) {
        prefs.edit().putString(KEY_CONFIG_URL, url).apply()
        emptyView.visibility = View.GONE
        loadConfig(url)
    }

    private fun promptConfigUrl() {
        val input = EditText(this).apply {
            setText(prefs.getString(KEY_CONFIG_URL, null) ?: SAMPLE_CONFIG)
            setSingleLine(true)
            hint = "https://..."
        }
        AlertDialog.Builder(this)
            .setTitle("配置地址")
            .setView(input)
            .setPositiveButton("确定") { _: DialogInterface, _: Int ->
                val url = input.text.toString().trim()
                if (url.startsWith("http")) importConfig(url)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun buildUi(): View {
        val dp = { v: Int -> Theme.dp(this, v) }
        val root = FrameLayout(this).apply { setBackgroundColor(Theme.BG) }
        val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(column, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        // 顶栏
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(24), dp(20), dp(24), dp(12))
        }
        top.addView(Theme.label(this, "QBOX", 26, Theme.TEXT, bold = true).apply {
            letterSpacing = 0.12f
        })
        top.addView(Theme.label(this, "  ·  点播", 15, Theme.TEXT_DIM))
        top.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(0, 1, 1f) })
        statusView = Theme.label(this, "加载配置…", 13, Theme.TEXT_DIM)
        top.addView(statusView)
        column.addView(top, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        // 站点网格
        grid = RecyclerView(this).apply {
            layoutManager = GridLayoutManager(this@HomeActivity, 5, RecyclerView.VERTICAL, false)
            setPadding(dp(20), dp(4), dp(20), dp(20))
            clipToPadding = false
        }
        column.addView(grid, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        // 空壳导入面板（无配置时显示）
        emptyView = buildEmpty()
        emptyView.visibility = View.GONE
        root.addView(emptyView, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        return root
    }

    private fun buildEmpty(): View {
        val dp = { v: Int -> Theme.dp(this, v) }
        val wrap = FrameLayout(this).apply { setBackgroundColor(Theme.BG) }
        val center = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }
        wrap.addView(center, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        center.addView(TextView(this).apply {
            text = "QBOX"
            textSize = 44f
            setTextColor(Theme.TEXT)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.16f
            gravity = Gravity.CENTER
        })
        center.addView(TextView(this).apply {
            text = "导入 TVBox 配置地址开始使用"
            textSize = 15f
            setTextColor(Theme.TEXT_DIM)
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, 0)
        })

        val input = EditText(this).apply {
            setText(SAMPLE_CONFIG)
            setSingleLine(true)
            textSize = 13f
            setTextColor(Theme.TEXT)
            hint = "https://..."
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = Theme.corner(context, Theme.SURFACE, 8f, 1, Theme.BORDER)
        }
        center.addView(input, LinearLayout.LayoutParams(dp(420), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(24)
        })

        val button = TextView(this).apply {
            text = "导  入"
            textSize = 15f
            setTextColor(Theme.TEXT)
            gravity = Gravity.CENTER
            isFocusable = true
            background = Theme.corner(context, Theme.SURFACE_HI, 999f)
            setOnFocusChangeListener { v, hasFocus ->
                v.background = if (hasFocus)
                    Theme.corner(context, Theme.SURFACE_HI, 999f, 2, Theme.ACCENT)
                else Theme.corner(context, Theme.SURFACE_HI, 999f)
                v.animate().scaleX(if (hasFocus) 1.05f else 1f)
                    .scaleY(if (hasFocus) 1.05f else 1f).setDuration(120).start()
            }
            setOnClickListener {
                val url = input.text.toString().trim()
                if (url.startsWith("http")) importConfig(url)
                else statusView.text = "地址须以 http(s) 开头"
            }
        }
        center.addView(button, LinearLayout.LayoutParams(dp(180), dp(44)).apply { topMargin = dp(18) })
        center.addView(TextView(this).apply {
            text = "菜单键可随时更换配置"
            textSize = 12f
            setTextColor(Theme.TEXT_FAINT)
            gravity = Gravity.CENTER
            setPadding(0, dp(14), 0, 0)
        })
        return wrap
    }

    private fun loadConfig(url: String) {
        statusView.text = "加载配置…"
        emptyView.visibility = View.GONE
        thread {
            runCatching {
                val config = ConfigRepository.load(url)
                val sites = config.sites.orEmpty()
                    .filter { it.type == 0 || it.type == 3 }   // M1：jar spider
                    .filter { !it.api.isNullOrBlank() }
                sites to config
            }.onSuccess { (sites, config) ->
                main.post { showSites(sites, config) }
            }.onFailure {
                main.post { statusView.text = "配置加载失败：${it.message?.take(50)}（菜单键可更换）" }
            }
        }
    }

    private fun showSites(sites: List<Site>, config: Config) {
        statusView.text = "${config.sites?.size ?: 0} 站点"
        grid.adapter = SiteAdapter(this, sites) { site ->
            val intent = Intent(this, VodActivity::class.java)
            intent.putExtra(EXTRA_SITE, SiteParcel.from(site, ConfigRepository.currentUrl ?: ""))
            startActivity(intent)
        }
        grid.requestFocus()
    }
}


class SiteAdapter(
    private val ctx: Context,
    private val sites: List<Site>,
    private val onClick: (Site) -> Unit,
) : RecyclerView.Adapter<SiteAdapter.Holder>() {

    class Holder(val view: TextView) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val dp = { v: Int -> Theme.dp(ctx, v) }
        val view = TextView(ctx).apply {
            textSize = 14f
            setTextColor(Theme.TEXT)
            gravity = Gravity.CENTER
            maxLines = 2
            setPadding(dp(10), dp(18), dp(10), dp(18))
        }
        Theme.bindFocus(view)
        return Holder(view)
    }

    override fun getItemCount(): Int = sites.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val site = sites[position]
        holder.view.text = site.name ?: site.key ?: "?"
        holder.view.setOnClickListener { onClick(site) }
    }
}
