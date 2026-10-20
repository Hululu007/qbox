package com.qbox.tv.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.qbox.tv.config.Config
import com.qbox.tv.config.ConfigRepository
import com.qbox.tv.config.Site
import com.qbox.tv.config.SiteParcel
import kotlin.concurrent.thread

/** 首页：加载配置 → 站点网格 → 进入点播。菜单键 = 更换配置地址。 */
class HomeActivity : Activity() {

    companion object {
        const val DEFAULT_CONFIG =
            "https://raw.githubusercontent.com/gaotianliuyun/gao/main/0826.json"
        const val EXTRA_SITE = "site"
        const val PREFS = "qbox"
        const val KEY_CONFIG_URL = "configUrl"
    }

    private lateinit var statusView: TextView
    private lateinit var grid: RecyclerView
    private lateinit var prefs: SharedPreferences
    private val main = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        setContentView(buildUi())
        loadConfig(prefs.getString(KEY_CONFIG_URL, null) ?: DEFAULT_CONFIG)
    }

    /** 菜单键：弹出配置地址输入框（电视上可用软键盘 / adb 配合）。 */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU) { promptConfigUrl(); return true }
        return super.onKeyDown(keyCode, event)
    }

    private fun promptConfigUrl() {
        val input = EditText(this).apply {
            setText(prefs.getString(KEY_CONFIG_URL, null) ?: DEFAULT_CONFIG)
            setSingleLine(true)
            hint = "https://..."
        }
        AlertDialog.Builder(this)
            .setTitle("配置地址")
            .setView(input)
            .setPositiveButton("确定") { _: DialogInterface, _: Int ->
                val url = input.text.toString().trim()
                if (url.startsWith("http")) {
                    prefs.edit().putString(KEY_CONFIG_URL, url).apply()
                    loadConfig(url)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun buildUi(): View {
        val dp = { v: Int -> Theme.dp(this, v) }
        val root = FrameLayout(this).apply { setBackgroundColor(Theme.BG) }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
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
        return root
    }

    private fun loadConfig(url: String) {
        setStatus("加载配置…")
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
                main.post { setStatus("配置加载失败：${it.message?.take(60)}") }
            }
        }
    }

    private fun showSites(sites: List<Site>, config: Config) {
        setStatus("${config.sites?.size ?: 0} 站点")
        grid.adapter = SiteAdapter(this, sites) { site ->
            val intent = Intent(this, VodActivity::class.java)
            intent.putExtra(EXTRA_SITE, SiteParcel.from(site, ConfigRepository.currentUrl ?: ""))
            startActivity(intent)
        }
        grid.requestFocus()
    }

    private fun setStatus(text: String) {
        statusView.text = text
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
