package com.qbox.tv.ui

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import coil.load

/** 海报卡片：2:3 图 + 标题 + 备注。 */
class PosterCard @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {

    private val image = ImageView(context).apply {
        scaleType = ImageView.ScaleType.CENTER_CROP
        setBackgroundColor(Theme.SURFACE)
    }
    private val title = TextView(context).apply {
        textSize = 13f
        setTextColor(Theme.TEXT)
        maxLines = 1
    }
    private val remark = TextView(context).apply {
        textSize = 11f
        setTextColor(Theme.TEXT_DIM)
        maxLines = 1
    }

    init {
        val dp = { v: Int -> Theme.dp(context, v) }
        orientation = VERTICAL
        val w = dp(150)
        val h = dp(225) // 2:3
        addView(image, LayoutParams(LayoutParams.MATCH_PARENT, h))
        addView(title, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            setMargins(0, dp(6), 0, 0)
        })
        addView(remark, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            setMargins(0, dp(1), 0, 0)
        })
        isFocusable = true
        descendantFocusability = FOCUS_BLOCK_DESCENDANTS
        Theme.bindFocus(this)
    }

    fun bind(name: String?, pic: String?, note: String?) {
        title.text = name ?: ""
        remark.text = note ?: ""
        if (pic.isNullOrBlank()) {
            image.setBackgroundColor(Theme.SURFACE)
            image.setImageResource(0)
        } else {
            image.load(pic) {
                crossfade(true)
                error(Color.TRANSPARENT)
            }
        }
    }
}
