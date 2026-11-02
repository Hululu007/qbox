package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.leanback.widget.OnChildViewHolderSelectedListener;
import androidx.leanback.widget.VerticalGridView;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.utils.KeyUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class CustomVerticalGridView extends VerticalGridView {

    private List<View> views;
    private boolean pressDown;
    private boolean pressUp;
    private boolean moveTop;
    private boolean hideHeaderEnabled = true;
    private int clipTop;

    public CustomVerticalGridView(@NonNull Context context) {
        this(context, null);
    }

    public CustomVerticalGridView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomVerticalGridView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setMoveTop(true);
    }

    @Override
    protected void initAttributes(@NonNull Context context, @Nullable AttributeSet attrs) {
        super.initAttributes(context, attrs);
        setOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(@NonNull RecyclerView parent, @Nullable ViewHolder child, int position, int subposition) {
                if (hideHeaderEnabled && pressDown && position == 1) hideHeader();
                if (hideHeaderEnabled && pressUp && position == 0) showHeader();
            }
        });
    }

    /**
     * 聚合搜索页不要收顶部标题/站点 tab：收了以后 pager 往上顶，
     * 第一排海报会贴到工具栏下面，焦点框上沿被裁掉。
     */
    public void setHideHeaderEnabled(boolean enabled) {
        this.hideHeaderEnabled = enabled;
    }

    public void setHeader(FragmentActivity activity, int... layoutIds) {
        if (activity != null) views = Arrays.stream(layoutIds).mapToObj(id -> (View) activity.findViewById(id)).filter(Objects::nonNull).toList();
    }

    public void setMoveTop(boolean moveTop) {
        this.moveTop = moveTop;
    }

    /**
     * 顶部绘制裁切线（px）：列表向上滚动时，卡片画到这里就被裁掉，
     * 不会穿到顶栏/分类条下面形成重影或「被 tab 盖住」。
     * 只裁绘制、不改布局，所以焦点放大和 padding 都照常。
     */
    public void setClipTop(int clipTop) {
        this.clipTop = clipTop;
    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        if (clipTop < 0) {
            super.dispatchDraw(canvas);
            return;
        }
        int save = canvas.save();
        canvas.clipRect(0, clipTop, getWidth(), getHeight());
        super.dispatchDraw(canvas);
        canvas.restoreToCount(save);
    }

    public void hideHeader() {
        if (views != null) for (View view : views) view.setVisibility(View.GONE);
    }

    public void showHeader() {
        if (views != null) for (View view : views) view.setVisibility(View.VISIBLE);
    }

    public boolean isHeaderVisible() {
        if (views != null) for (View view : views) if (view.getId() == R.id.recycler && view.getVisibility() == View.VISIBLE) return true;
        return false;
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        if (!KeyUtil.isActionDown(event)) return super.dispatchKeyEvent(event);
        if (KeyUtil.isBackKey(event)) return moveTop && moveToTop();
        pressUp = KeyUtil.isUpKey(event);
        pressDown = KeyUtil.isDownKey(event);
        return super.dispatchKeyEvent(event);
    }

    public boolean moveToTop() {
        if (views == null || getSelectedPosition() == 0 || getAdapter() == null || getAdapter().getItemCount() == 0) return false;
        for (View view : views) if (view.getId() == R.id.recycler) view.requestFocus();
        scrollToPosition(0);
        showHeader();
        return true;
    }
}
