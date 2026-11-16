package com.fongmi.android.tv.bean;

import com.google.gson.annotations.SerializedName;

/**
 * 内容源下发的跳过片段（片头/片尾/中段），FongMi 上游 backport。
 * 由 {@code Result.skipSegments} 提供，{@code VodSkipPolicy} 用于起播与播放中跳转。
 */
public class SkipSegment {

    @SerializedName("type")
    private String type;
    @SerializedName("start")
    private Long start;
    @SerializedName("end")
    private Long end;

    public boolean isOpening() {
        return "opening".equals(type);
    }

    public boolean isMiddle() {
        return "middle".equals(type);
    }

    public boolean isEnding() {
        return "ending".equals(type);
    }

    public long getStart() {
        return start == null ? -1 : start;
    }

    public long getEnd() {
        return end == null ? -1 : end;
    }

    public boolean isToEnd() {
        return end == null;
    }

    public boolean isValid() {
        if (getStart() < 0 || (!isOpening() && !isMiddle() && !isEnding())) return false;
        return isToEnd() ? isEnding() && getStart() > 0 : getEnd() > getStart();
    }

    public boolean contains(long position) {
        return isValid() && position >= getStart() && (isToEnd() || position < getEnd());
    }
}
