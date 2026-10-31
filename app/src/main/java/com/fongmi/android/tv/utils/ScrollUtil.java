package com.fongmi.android.tv.utils;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class ScrollUtil {

    /**
     * 内容是否已经明显离开顶部。
     * Leanback 的 GridLayoutManager 不更新 scrollOffset，{@link RecyclerView#canScrollVertically(int)}
     * 在行对齐滚动时始终返回 false，所以用「首个可见 child 的位置 / top」来判断；
     * 同时留 6dp 容差，避免首行 1px 的亚像素偏移就把顶部渐隐点亮、盖住第一行标题。
     */
    public static boolean isScrolledFromTop(@NonNull RecyclerView recycler) {
        if (recycler.getLayoutManager() == null || recycler.getChildCount() == 0) return false;
        if (recycler.getLayoutManager().getPosition(recycler.getChildAt(0)) > 0) return true;
        int tolerance = Math.max(2, ResUtil.dp2px(6));
        return recycler.getChildAt(0).getTop() < recycler.getPaddingTop() - tolerance;
    }
}
