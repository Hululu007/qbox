package com.fongmi.android.tv.ui.custom;

import android.app.Activity;
import android.os.SystemClock;
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
    private long lastSeekTick;
    private long holdStartTick;

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
     * QBox 1.9.10：快进改成阶梯加速；1.9.20 起按「按住时长」分档；第 20 轮重做斜坡（N 指标）：
     * 起步必须是秒级细调——前 3 秒全程 ±1s（130ms 密节拍，按下即 +1s 而非 +10s），
     * 3 秒后才逐步放大幅度（10s → 30s → 60s → 长片尾部 120s/180s/300s）。
     * 档位表：{阶段上限 ms, 步长 ms, 最小节拍 ms}；节拍随档位放疏，防止大档连跳失控。
     */
    private static final long[][] RAMP = {
            {3_000, 1_000, 130},
            {5_000, 10_000, 500},
            {7_000, 30_000, 900},
            {10_000, 60_000, 700},
            {14_000, 120_000, 300},
            {18_000, 180_000, 300},
            {Long.MAX_VALUE, 300_000, 300},
    };

    private void seek(KeyEvent event, boolean forward) {
        int repeat = event.getRepeatCount();
        long now = SystemClock.uptimeMillis();
        if (repeat == 0) holdStartTick = now;
        long elapsed = now - holdStartTick;
        if (repeat > 0 && now - lastSeekTick < rampAt(elapsed)[2]) return;
        lastSeekTick = now;
        long step = rampAt(elapsed)[1];
        holdTime += forward ? step : -step;
        listener.onSeeking(holdTime);
    }

    /** 遥控器 FF/REW 由 PlaybackService 转发（媒体键被系统路由给 MediaSession，Activity 收不到 DOWN），与方向键共用同一斜坡。 */
    public void onMediaKey(KeyEvent event, boolean forward) {
        if (KeyUtil.isActionDown(event)) seek(event, forward);
        else if (KeyUtil.isActionUp(event)) App.post(() -> listener.onSeekEnd(holdTime), 250);
    }

    private long[] rampAt(long elapsedMs) {
        for (long[] stage : RAMP) if (elapsedMs < stage[0]) return stage;
        return RAMP[RAMP.length - 1];
    }

    public void reset() {
        holdTime = 0;
        lastSeekTick = 0;
        holdStartTick = 0;
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
