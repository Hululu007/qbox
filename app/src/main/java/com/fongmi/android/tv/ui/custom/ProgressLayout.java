package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.fongmi.android.tv.databinding.ViewEmptyBinding;
import com.fongmi.android.tv.databinding.ViewProgressBinding;

import java.util.ArrayList;
import java.util.List;

public class ProgressLayout extends RelativeLayout {

    private static final String TAG_PROGRESS = "ProgressLayout.TAG_PROGRESS";

    public enum State {
        CONTENT, PROGRESS, EMPTY
    }

    private List<View> mContentViews;
    private View mProgressView;
    private View mEmptyView;
    private ViewEmptyBinding mEmptyBinding;
    private ViewProgressBinding mProgressBinding;
    private State mState;
    private boolean mCubeLoaded;

    public ProgressLayout(Context context) {
        super(context);
    }

    public ProgressLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ProgressLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        mState = State.CONTENT;
        mContentViews = new ArrayList<>();
        initView();
    }

    private void initView() {
        mEmptyBinding = ViewEmptyBinding.inflate(LayoutInflater.from(getContext()));
        mEmptyView = mEmptyBinding.getRoot();
        mEmptyView.setTag(TAG_PROGRESS);
        mEmptyView.setVisibility(GONE);
        mProgressBinding = ViewProgressBinding.inflate(LayoutInflater.from(getContext()));
        mProgressView = mProgressBinding.getRoot();
        mProgressView.setTag(TAG_PROGRESS);
        mProgressView.setVisibility(GONE);
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(CENTER_IN_PARENT);
        addView(mProgressView, params);
        addView(mEmptyView, params);
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);
        if (child.getTag() == null || !child.getTag().equals(TAG_PROGRESS)) {
            mContentViews.add(child);
        }
    }

    public void showProgress() {
        // 立方体载入动画（第 33/34 轮）：首次进入加载态才解码，不进冷启动关键路径。
        // API 28+ 用 ImageDecoder 原生解码透明动态 WebP（AnimatedImageDrawable 自播放，不加依赖）；
        // API 24-27 无 ImageDecoder，退回静态立方体 logo。
        if (!mCubeLoaded && mProgressBinding != null) {
            mCubeLoaded = true;
            try {
                if (android.os.Build.VERSION.SDK_INT >= 28) {
                    android.graphics.ImageDecoder.Source src = android.graphics.ImageDecoder.createSource(getResources(), com.fongmi.android.tv.R.raw.qbox_loading);
                    android.graphics.drawable.Drawable d = android.graphics.ImageDecoder.decodeDrawable(src, (decoder, info, source) -> { int p = cubeTargetPx(); decoder.setTargetSize(p, p); });
                    mProgressBinding.loadingCube.setImageDrawable(d);
                    if (d instanceof android.graphics.drawable.AnimatedImageDrawable) ((android.graphics.drawable.AnimatedImageDrawable) d).start();
                } else {
                    mProgressBinding.loadingCube.setImageResource(com.fongmi.android.tv.R.drawable.ic_logo);
                }
            } catch (Throwable ignored) {
                mProgressBinding.loadingCube.setImageResource(com.fongmi.android.tv.R.drawable.ic_logo);
            }
        }
        switchState(State.PROGRESS);
    }

    /** WebP 目标解码边长：112dp 视图 × 密度，解码期降采（38 帧 640² 全尺寸解码无必要）。 */
    private int cubeTargetPx() {
        return (int) (112 * getResources().getDisplayMetrics().density);
    }

    public void showEmpty() {
        switchState(State.EMPTY);
    }

    /** 空状态文案分场景，例如收藏页与分类页提示不同；hint 为空时隐藏副标题。 */
    public void showEmpty(CharSequence text, CharSequence hint) {
        if (mEmptyBinding != null) {
            if (text != null) mEmptyBinding.text.setText(text);
            mEmptyBinding.hint.setVisibility(hint == null || hint.length() == 0 ? View.GONE : View.VISIBLE);
            if (hint != null && hint.length() > 0) mEmptyBinding.hint.setText(hint);
        }
        if (mState == State.EMPTY) {
            mEmptyView.setVisibility(VISIBLE);
            setContentVisibility(false);
        } else {
            switchState(State.EMPTY);
        }
    }

    public void showContent() {
        switchState(State.CONTENT);
    }

    public void showContent(boolean flag, int size) {
        if (flag && size == 0) showEmpty();
        else showContent();
    }

    public boolean isProgress() {
        return mState == State.PROGRESS;
    }

    public boolean isContent() {
        return mState == State.CONTENT;
    }

    public boolean isEmpty() {
        return mState == State.EMPTY;
    }

    public void switchState(State state) {
        if (mState == state) return;
        mState = state;
        switch (state) {
            case CONTENT:
                mEmptyView.setVisibility(GONE);
                mProgressView.setVisibility(GONE);
                setContentVisibility(true);
                break;
            case PROGRESS:
                mEmptyView.setVisibility(GONE);
                mProgressView.setVisibility(VISIBLE);
                setContentVisibility(false);
                break;
            case EMPTY:
                mEmptyView.setVisibility(VISIBLE);
                mProgressView.setVisibility(GONE);
                setContentVisibility(false);
                break;
        }
    }

    private void setContentVisibility(boolean visible) {
        for (View view : mContentViews) {
            if (visible) showView(view);
            else hideView(view);
        }
    }

    private void showView(View view) {
        view.setAlpha(0f);
        view.setVisibility(VISIBLE);
        view.animate().alpha(1f).setDuration(100);
    }

    private void hideView(View view) {
        view.setVisibility(INVISIBLE);
    }
}
