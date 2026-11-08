package com.fongmi.android.tv.utils;

import android.view.View;

import com.fongmi.android.tv.BuildConfig;
import com.fongmi.android.tv.R;
import com.github.catvod.utils.Prefers;

/**
 * 四边收边力度档（第 30 轮对比用，DEBUG）：
 * `adb shell am broadcast -a com.qbox.app.DEBUG_BACKDROP --ei t <0|1|2>`，0=极轻(40dp/12%) 1=中(64dp/20%) 2=稍强(96dp/30%)。
 * 详情页仅在 D 版下生效；首页/分类页直接作用于统一收边层。Prefers 持久化。
 */
public class EdgeTiers {

    public static final int[] TIERS = {R.drawable.shape_backdrop_scrim_d, R.drawable.shape_backdrop_scrim_d2, R.drawable.shape_backdrop_scrim_d3};

    public static int get() {
        return Math.max(0, Math.min(TIERS.length - 1, Prefers.getInt("debug_edge_tier", 0)));
    }

    public static void apply(View scrim, int tier) {
        int t = Math.max(0, Math.min(TIERS.length - 1, tier));
        scrim.setBackgroundResource(TIERS[t]);
        if (BuildConfig.DEBUG) Prefers.put("debug_edge_tier", t);
    }
}
