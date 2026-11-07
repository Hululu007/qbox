package com.fongmi.android.tv.ui.custom;

import android.app.Activity;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;

import androidx.annotation.NonNull;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.utils.KeyUtil;

public class CustomKeyDownVod extends GestureDetector.SimpleOnGestureListener {

    private final GestureDetector detector;
    private final Listener listener;
    private boolean changeSpeed;
    private boolean full;
    private long holdTime;
    /** 进度条拖动模式（第 37 轮）：repeat0 不落位移（首步让给「单点 ±1%」判定），hold 从首个 repeat 起按同一曲线走。 */
    private boolean scrubMode;

    public void setScrubMode(boolean scrub) {
        scrubMode = scrub;
        if (!scrub) reset();
    }

    public static CustomKeyDownVod create(Activity activity) {
        return new CustomKeyDownVod(activity);
    }

    private CustomKeyDownVod(Activity activity) {
        this.detector = new GestureDetector(activity, this);
        this.listener = (Listener) activity;
    }

    public boolean onTouchEvent(MotionEvent e) {
        if (!full) return false;
        return detector.onTouchEvent(e);
    }

    public void setFull(boolean full) {
        this.full = full;
    }

    public boolean hasEvent(KeyEvent event) {
        return KeyUtil.isEnterKey(event) || KeyUtil.isUpKey(event) || KeyUtil.isDownKey(event) || KeyUtil.isLeftKey(event) || KeyUtil.isRightKey(event);
    }

    public boolean onKeyDown(KeyEvent event) {
        check(event);
        return true;
    }

    private void check(KeyEvent event) {
        if (KeyUtil.isActionDown(event) && KeyUtil.isLeftKey(event)) {
            seek(event, false);
        } else if (KeyUtil.isActionDown(event) && KeyUtil.isRightKey(event)) {
            seek(event, true);
        } else if (KeyUtil.isActionUp(event) && (KeyUtil.isLeftKey(event) || KeyUtil.isRightKey(event))) {
            App.post(() -> listener.onSeekEnd(holdTime), 250);
        } else if (KeyUtil.isActionUp(event) && KeyUtil.isUpKey(event)) {
            if (changeSpeed) listener.onSpeedEnd();
            else listener.onKeyUp();
            changeSpeed = false;
        } else if (KeyUtil.isActionUp(event) && KeyUtil.isDownKey(event)) {
            listener.onKeyDown();
        } else if (KeyUtil.isActionUp(event) && KeyUtil.isEnterKey(event)) {
            listener.onKeyCenter();
        } else if (event.isLongPress() && KeyUtil.isUpKey(event)) {
            listener.onSpeedUp();
            changeSpeed = true;
        }
    }

    @Override
    public boolean onDoubleTap(@NonNull MotionEvent e) {
        listener.onDoubleTap();
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
        listener.onSingleTap();
        return true;
    }

    /**
     * 第 38 轮（P1-1，用户拍板）：快进/快退回归 stock fongmi 原生手感——
     * 每个按键事件（含 repeat）固定 ±Constant.INTERVAL_SEEK（10s），无曲线、无档位、无爬升，
     * 瞬时响应、松开 250ms 落位；QBox 自造的三代模型（阶梯/分档/连续曲线）全部移除。
     * 仅保留一处自有逻辑 = 进度条拖动模式（scrubMode）：repeat0 不位移（让位 Activity 侧「单点 ±1%」判定），
     * 按住后每 repeat ±max(2×INTERVAL_SEEK, 1% 片长)——与原生同一套「每事件固定步长」机制，
     * 但任何时刻步幅 ≥2× 全局快进（P0-4 要求）；片长仅用于该百分比步长。
     */
    private long durationMs;

    public void setDuration(long ms) {
        durationMs = Math.max(0, ms);
    }

    private void seek(KeyEvent event, boolean forward) {
        if (scrubMode && event.getRepeatCount() == 0) {
            // 拖动模式首 tick 不位移：等 UP 判单点（Activity 侧 ±1%）、等 repeat 判长按。
            if (com.fongmi.android.tv.BuildConfig.DEBUG) {
                android.util.Log.d("QBoxSeek", "holdStart dur=" + durationMs + " scrub");
            }
            return;
        }
        // 原生：每事件固定 ±INTERVAL_SEEK；拖动模式：±max(2×INTERVAL_SEEK, 1% 片长)。
        long step = com.fongmi.android.tv.Constant.INTERVAL_SEEK;
        if (scrubMode) step = Math.max(step * 2, durationMs / 100);
        holdTime += forward ? step : -step;
        if (com.fongmi.android.tv.BuildConfig.DEBUG) {
            android.util.Log.d("QBoxSeek", "tick step=" + step + " cum=" + holdTime + " dir=" + (forward ? "FF" : "REW") + (scrubMode ? " scrub" : ""));
        }
        listener.onSeeking(holdTime);
    }

    /** 遥控器 FF/REW 由 PlaybackService 转发（媒体键被系统路由给 MediaSession，Activity 收不到 DOWN），与方向键共用同一原生步长。 */
    public void onMediaKey(KeyEvent event, boolean forward) {
        if (KeyUtil.isActionDown(event)) seek(event, forward);
        else if (KeyUtil.isActionUp(event)) App.post(() -> listener.onSeekEnd(holdTime), 250);
    }

    public void reset() {
        holdTime = 0;
    }

    public interface Listener {

        void onSeeking(long time);

        void onSeekEnd(long time);

        void onSpeedUp();

        void onSpeedEnd();

        void onKeyUp();

        void onKeyDown();

        void onKeyCenter();

        void onSingleTap();

        void onDoubleTap();
    }
}
