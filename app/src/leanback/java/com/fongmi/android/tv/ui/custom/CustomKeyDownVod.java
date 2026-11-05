package com.fongmi.android.tv.ui.custom;

import android.app.Activity;
import android.os.SystemClock;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;

import androidx.annotation.NonNull;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Constant;
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
     * QBox 1.9.10：快进改成阶梯加速；1.9.20 起改为按「按住时长」分档（C9）。
     * 旧版按 repeat 计数分档，但 repeat 事件约 50ms 一个、远快于 300ms 节流后的实际步进，
     * 按住约 0.2s 就进入 20s 档，一松手飞过头。现按按住时长：
     * 前 2.5s 每步 10s（细调窗口，GS-42 建议由 1.5s 延长）→ 4.5s 内 20s → 7s 内 30s → 10s 内 60s → 14s 内 120s → 18s 内 180s → 300s。
     */
    private void seek(KeyEvent event, boolean forward) {
        int repeat = event.getRepeatCount();
        long now = SystemClock.uptimeMillis();
        if (repeat == 0) holdStartTick = now;
        // 长按的 repeat 事件来得很密：先 300ms 一步，别让第一次长按就飞过头。
        if (repeat > 0 && now - lastSeekTick < 300) return;
        lastSeekTick = now;
        long step = stepFor(now - holdStartTick);
        holdTime += forward ? step : -step;
        listener.onSeeking(holdTime);
    }

    private long stepFor(long elapsedMs) {
        if (elapsedMs < 2_500) return Constant.INTERVAL_SEEK;
        if (elapsedMs < 4_500) return 20_000;
        if (elapsedMs < 7_000) return 30_000;
        if (elapsedMs < 10_000) return 60_000;
        if (elapsedMs < 14_000) return 120_000;
        if (elapsedMs < 18_000) return 180_000;
        return 300_000;
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
