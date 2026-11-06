package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;

public class CustomViewPager extends ViewPager {

    public CustomViewPager(@NonNull Context context) {
        super(context);
    }

    public CustomViewPager(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setCurrentItem(int item) {
        super.setCurrentItem(item, false);
    }

    /**
     * 第 32 轮：禁用方向键翻页。ViewPager 默认在 DPAD_LEFT/RIGHT 无处可走时 arrowScroll 翻页——
     * 实测内容行（海报行/filter 行）长按到头会连翻几个大 tab。换大 tab 只能走 tab 行（点击切页），
     * pager 永不响应方向键翻页。
     */
    @Override
    public boolean arrowScroll(int direction) {
        return false;
    }
}
