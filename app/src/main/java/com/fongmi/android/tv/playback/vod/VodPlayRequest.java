package com.fongmi.android.tv.playback.vod;

import android.text.TextUtils;

import androidx.media3.common.C;

import com.fongmi.android.tv.bean.Episode;
import com.fongmi.android.tv.bean.Flag;
import com.fongmi.android.tv.bean.Result;

/**
 * 播放请求是<b>值对象</b>：起播位置随请求一起定死（FongMi 主线设计，GS-81①）。
 * 不要让 startPlayback 到开播那一刻才去读全局可变状态，否则任何路径在
 * 「请求发出后、开播前」改历史都会污染这一集的起点。
 */
public final class VodPlayRequest {

    private final String flag;
    private final String key;
    private final String id;
    private final long startPositionMs;

    private VodPlayRequest(String key, String flag, String id, long startPositionMs) {
        this.flag = flag == null ? "" : flag;
        this.key = key == null ? "" : key;
        this.id = id == null ? "" : id;
        this.startPositionMs = startPositionMs;
    }

    /** 未显式指定起点：由 startPositionMs()（每集独立进度）决定。 */
    public static VodPlayRequest create(String key, Flag flag, Episode episode) {
        return create(key, flag, episode, C.TIME_UNSET);
    }

    public static VodPlayRequest create(String key, Flag flag, Episode episode, long startPositionMs) {
        return new VodPlayRequest(key, flag.getFlag(), episode.getUrl(), startPositionMs);
    }

    public String getFlag() {
        return flag;
    }

    public String getKey() {
        return key;
    }

    public String getId() {
        return id;
    }

    public long getStartPositionMs() {
        return startPositionMs;
    }

    public boolean matches(String key, Flag flag, Episode episode) {
        return TextUtils.equals(this.key, key) && flag != null && episode != null && TextUtils.equals(this.flag, flag.getFlag()) && TextUtils.equals(this.id, episode.getUrl());
    }

    public boolean matches(VodPlayRequest request) {
        return request != null && TextUtils.equals(key, request.key) && TextUtils.equals(flag, request.flag) && TextUtils.equals(id, request.id);
    }

    public boolean accepts(Result result) {
        return result != null && (result.getFlag().isEmpty() || TextUtils.equals(flag, result.getFlag()));
    }
}
