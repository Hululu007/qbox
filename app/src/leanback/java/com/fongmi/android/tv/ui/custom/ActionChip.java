package com.fongmi.android.tv.ui.custom;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.content.ContextCompat;

import com.fongmi.android.tv.R;

/**
 * 首页动作按钮：静止时是一颗 46dp 的图标圆药丸，聚焦时横向生长出「图标 + 文字」。
 * 文字写在药丸内部，不占用图标下方一行，所以整排图标不会跳动。
 *
 * 展开有动画（180ms），收回必须瞬间归零：否则上一颗边收边挤，会把整排图标推来推去。
 */
public class ActionChip extends LinearLayoutCompat {

    private static final int EXPAND_DURATION = 180;

    private final ImageView icon;
    private final TextView label;
    private ValueAnimator animator;
    private int expandedWidth;
    private boolean expanded;

    public ActionChip(Context context) {
        this(context, null);
    }

    public ActionChip(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ActionChip(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setClickable(true);
        setFocusable(true);
        setFocusableInTouchMode(true);
        setBackgroundResource(R.drawable.selector_action_chip);
        int padding = dp(12);
        setPadding(padding, 0, padding, 0);

        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.ActionChip);
        Drawable drawable = array.getDrawable(R.styleable.ActionChip_chipIcon);
        CharSequence text = array.getText(R.styleable.ActionChip_chipText);
        array.recycle();

        icon = new ImageView(context);
        icon.setLayoutParams(new LayoutParams(dp(22), dp(22)));
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        icon.setImageDrawable(drawable);
        icon.setDuplicateParentStateEnabled(true);
        icon.setImageTintList(ContextCompat.getColorStateList(context, R.color.text));
        addView(icon);

        label = new TextView(context);
        LayoutParams params = new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMarginStart(dp(8));
        label.setLayoutParams(params);
        label.setText(text);
        label.setTextSize(14);
        label.setSingleLine(true);
        label.setIncludeFontPadding(false);
        label.setGravity(Gravity.CENTER_VERTICAL);
        label.setDuplicateParentStateEnabled(true);
        label.setTextColor(ContextCompat.getColorStateList(context, R.color.text));
        label.setVisibility(View.GONE);
        addView(label);

        if (text != null) setContentDescription(text);
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, @Nullable Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        if (gainFocus) expand();
        else collapse();
    }

    private void expand() {
        if (expanded) return;
        expanded = true;
        // GONE 的 View 不会被重新测量，getMeasuredWidth() 会保留上一次的值（正好是展开后的宽度），
        // 从这里起动画会得到 from == to，看起来就是「没有动画」。所以每次都从 0 开始。
        if (expandedWidth <= 0) expandedWidth = measureLabel() + dp(8);
        setLabelWidth(0);
        label.setVisibility(View.VISIBLE);
        animate(expandedWidth);
    }

    private void collapse() {
        if (!expanded) return;
        expanded = false;
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
        setLabelWidth(0);
        label.setVisibility(View.GONE);
    }

    private int measureLabel() {
        CharSequence text = label.getText();
        if (text == null || text.length() == 0) return 0;
        return (int) Math.ceil(label.getPaint().measureText(text, 0, text.length())) + dp(2);
    }

    private void setLabelWidth(int width) {
        ViewGroup.LayoutParams params = label.getLayoutParams();
        if (params.width == width) return;
        params.width = width;
        label.setLayoutParams(params);
    }

    private void animate(int to) {
        animator = ValueAnimator.ofInt(0, to);
        animator.setDuration(EXPAND_DURATION);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> setLabelWidth((int) animation.getAnimatedValue()));
        animator.start();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
