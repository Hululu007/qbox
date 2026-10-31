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
 * 首页动作按钮：静止态是一个 46dp 的图标圆药丸，聚焦时横向展开成「图标 + 文字」。
 * 文字写在药丸内部，不再占用图标下方一行，工具栏更紧凑，动态也更有质感。
 */
public class ActionChip extends LinearLayoutCompat {

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
        if (expandedWidth <= 0) expandedWidth = measureLabel() + dp(8);
        label.setVisibility(View.VISIBLE);
        animateWidth(label.getMeasuredWidth(), expandedWidth);
    }

    private void collapse() {
        if (!expanded) return;
        expanded = false;
        animateWidth(label.getLayoutParams().width, 0);
    }

    private int measureLabel() {
        CharSequence text = label.getText();
        if (text == null || text.length() == 0) return 0;
        return (int) Math.ceil(label.getPaint().measureText(text, 0, text.length())) + dp(2);
    }

    private void animateWidth(int from, int to) {
        if (animator != null) animator.cancel();
        animator = ValueAnimator.ofInt(from, to);
        animator.setDuration(180);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            ViewGroup.LayoutParams params = label.getLayoutParams();
            params.width = (int) animation.getAnimatedValue();
            label.setLayoutParams(params);
        });
        animator.start();
        if (to == 0) postDelayed(() -> {
            if (!expanded) label.setVisibility(View.GONE);
        }, 190);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
