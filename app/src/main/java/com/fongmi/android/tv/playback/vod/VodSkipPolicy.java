package com.fongmi.android.tv.playback.vod;

import androidx.media3.common.C;

import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.SkipSegment;

/**
 * 起播位置与跳过片段策略，FongMi 上游 backport（GS-81②）。
 *
 * <p>把「片头秒数 / 自动跳过 / 源站下发起点 / skipSegments 片头」集中到一处，
 * 避免每条播放规则都直接改 VodHistoryPolicy.startPositionMs。
 * resumePositionMs 由 {@link VodHistoryPolicy#startPositionMs} 提供（每集独立进度）。
 */
public final class VodSkipPolicy {

    private VodSkipPolicy() {
    }

    public static long startPositionMs(History history, Result result, long resumePositionMs) {
        long opening = history == null ? C.TIME_UNSET : history.getOpening();
        boolean autoSkip = History.isAutoSkip(opening);
        long position = Math.max(autoSkip ? C.TIME_UNSET : opening, resumePositionMs);
        if (result == null) return position;
        if (result.hasPosition()) position = Math.max(position, result.getPosition());
        if (!autoSkip) return position;
        for (SkipSegment segment : result.getSkipSegments()) {
            if (segment != null && segment.isOpening() && segment.isValid() && segment.getStart() == 0) {
                position = Math.max(position, segment.getEnd());
            }
        }
        return position;
    }

    /**
     * 片尾是否到达（对齐 FongMi `skipToEnd`，GS-84）。
     *
     * <p>FongMi 原版在 `onTimeChanged` 与 `skipToEnd` 都带 `duration > 0` 守卫；
     * 我们的 fork 把这层守卫弄丢了——`duration` 未就绪（<=0）时 `ending + position >= duration`
     * 恒真，会在播放器首帧/切集未就绪时误触发「自动下一集」甚至连跳。
     * 这里恢复守卫并抽成纯函数，便于单测回归。
     */
    public static boolean isEndingReached(History history, long position, long duration) {
        return history != null && duration > 0 && history.getEnding() > 0 && history.getEnding() + position >= duration;
    }

    /** 播放中命中的可跳过片段（片头/片尾需开启自动跳过，中段总是跳过）。 */
    public static SkipSegment activeSegment(History history, Result result, long positionMs) {
        if (positionMs < 0 || result == null) return null;
        long opening = history == null ? C.TIME_UNSET : history.getOpening();
        long ending = history == null ? C.TIME_UNSET : history.getEnding();
        for (SkipSegment segment : result.getSkipSegments()) {
            if (segment == null || !segment.contains(positionMs)) continue;
            if (segment.isMiddle()) return segment;
            if (segment.isOpening() && History.isAutoSkip(opening)) return segment;
            if (segment.isEnding() && History.isAutoSkip(ending)) return segment;
        }
        return null;
    }
}
