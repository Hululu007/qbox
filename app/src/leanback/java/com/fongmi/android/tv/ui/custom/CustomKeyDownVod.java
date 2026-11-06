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
     * QBox 1.9.10：快进改成阶梯加速；1.9.20 起按「按住时长」分档；第 20 轮重做斜坡；第 23 轮放缓后档：
     * 起步不变——前 3 秒全程 ±1s（130ms 节拍，GS-42）；3 秒后步子整体放缓（5s→10s→25s→50s→100s→180s），
     * 每次跨档后 BLEND_MS 内按「速率」线性爬升（上一档速率→本档速率），过渡窗内 150ms 密集采样，
     * 手感是平滑加速而非台阶式突变；过渡窗外回到固定步长 + 随档放疏的节拍，防止大档连跳失控。
     * 档位表：{阶段上限 ms, 步长 ms, 最小节拍 ms}；档速率 = 步长/节拍。
     */
    private static final long BLEND_MS = 800;
    private static final long BLEND_TICK = 150;
    private static final long[][] RAMP = {
            {3_000, 1_000, 130},
            {5_000, 5_000, 600},
            {7_000, 10_000, 700},
            {10_000, 25_000, 800},
            {14_000, 50_000, 700},
            {18_000, 100_000, 500},
            {Long.MAX_VALUE, 180_000, 400},
    };

    private void seek(KeyEvent event, boolean forward) {
        long now = SystemClock.uptimeMillis();
        if (event.getRepeatCount() == 0) {
            // 按下首 tick 即 ±1s（GS-42 细调起步，第 23 轮不动）。
            holdStartTick = now;
            lastSeekTick = now;
            holdTime += forward ? 1_000 : -1_000;
            listener.onSeeking(holdTime);
            return;
        }
        long elapsed = now - holdStartTick;
        int stage = stageAt(elapsed);
        if (now - lastSeekTick < tickAt(stage, elapsed)) return;
        long dt = now - lastSeekTick;
        lastSeekTick = now;
        long step = stepAt(stage, elapsed, dt);
        holdTime += forward ? step : -step;
        listener.onSeeking(holdTime);
    }

    /** 遥控器 FF/REW 由 PlaybackService 转发（媒体键被系统路由给 MediaSession，Activity 收不到 DOWN），与方向键共用同一斜坡。 */
    public void onMediaKey(KeyEvent event, boolean forward) {
        if (KeyUtil.isActionDown(event)) seek(event, forward);
        else if (KeyUtil.isActionUp(event)) App.post(() -> listener.onSeekEnd(holdTime), 250);
    }

    private int stageAt(long elapsedMs) {
        for (int i = 0; i < RAMP.length; i++) if (elapsedMs < RAMP[i][0]) return i;
        return RAMP.length - 1;
    }

    private double rateOf(int stage) {
        return RAMP[stage][1] / (double) RAMP[stage][2];
    }

    /** 跨档后 BLEND_MS 内按速率线性爬升（速率×实际间隔），窗外用本档固定步长。 */
    private long stepAt(int stage, long elapsedMs, long dtMs) {
        if (stage == 0) return RAMP[0][1];
        long tIn = elapsedMs - RAMP[stage - 1][0];
        if (tIn >= BLEND_MS) return RAMP[stage][1];
        double rate = rateOf(stage - 1) + (rateOf(stage) - rateOf(stage - 1)) * tIn / (double) BLEND_MS;
        return Math.max(1, (long) (rate * dtMs));
    }

    /** 过渡窗内 150ms 密集采样（速率爬升被真实采到），窗外用本档节拍。 */
    private long tickAt(int stage, long elapsedMs) {
        if (stage > 0 && elapsedMs - RAMP[stage - 1][0] < BLEND_MS) return BLEND_TICK;
        return RAMP[stage][2];
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
