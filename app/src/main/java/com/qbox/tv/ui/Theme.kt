package com.qbox.tv.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.View
import android.widget.TextView

/**
 * QBox 设计语言：扁平克制硬朗工业暗色风。
 * 近黑背景、无大圆角、药丸按钮、焦点 = 亮边框 + 轻缩放、像素对齐。
 * 全代码构建，无 XML 布局。
 */
object Theme {
    val BG = Color.parseColor("#0A0A0C")
    val SURFACE = Color.parseColor("#141418")
    val SURFACE_HI = Color.parseColor("#1E1E24")
    val TEXT = Color.parseColor("#E8E8EC")
    val TEXT_DIM = Color.parseColor("#8A8A93")
    val TEXT_FAINT = Color.parseColor("#4A4A52")
    val BORDER = Color.parseColor("#2A2A32")
    val ACCENT = Color.parseColor("#E8E8EC")
    val RADIUS = 8f

    fun dp(ctx: Context, v: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), ctx.resources.displayMetrics).toInt()

    fun dpf(ctx: Context, v: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, ctx.resources.displayMetrics)

    fun corner(ctx: Context, color: Int, radiusDp: Float, strokeDp: Int = 0, strokeColor: Int = Color.TRANSPARENT): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dpf(ctx, radiusDp)
            if (strokeDp > 0) setStroke(dp(ctx, strokeDp), strokeColor)
        }

    fun label(ctx: Context, text: String, size: Int, color: Int, bold: Boolean = false): TextView =
        TextView(ctx).apply {
            this.text = text
            textSize = size.toFloat()
            setTextColor(color)
            if (bold) typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

    /** 统一焦点视觉：亮边框 + 轻放大。 */
    fun bindFocus(view: View, radiusDp: Float = RADIUS, scale: Float = 1.04f) {
        val ctx = view.context
        view.background = corner(ctx, SURFACE, radiusDp)
        view.setOnFocusChangeListener { _, hasFocus ->
            view.background = if (hasFocus) corner(ctx, SURFACE_HI, radiusDp, 2, ACCENT)
            else corner(ctx, SURFACE, radiusDp)
            view.animate().scaleX(if (hasFocus) scale else 1f).scaleY(if (hasFocus) scale else 1f)
                .setDuration(120).start()
        }
    }
}
