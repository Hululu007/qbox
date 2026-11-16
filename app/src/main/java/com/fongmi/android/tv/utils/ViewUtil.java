package com.fongmi.android.tv.utils;

import android.view.View;
import android.view.ViewGroup;

/**
 * 视图层级工具（消除 4 份 isInside / 2 份 findFocusable 的重复实现）。
 */
public final class ViewUtil {

    private ViewUtil() {
    }

    /** view 是否在 ancestor 的子树内（含自身）。 */
    public static boolean isInside(View ancestor, View view) {
        while (view != null) {
            if (view == ancestor) return true;
            view = view.getParent() instanceof View ? (View) view.getParent() : null;
        }
        return false;
    }

    /** 深度优先找第一个可见且可聚焦的控件。 */
    public static View findFirstFocusable(View view) {
        if (view == null || view.getVisibility() != View.VISIBLE) return null;
        if (view.isFocusable() && view.isEnabled()) return view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = findFirstFocusable(group.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }
}