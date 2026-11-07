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
     * QBox 1.9.10：快进改成阶梯加速；1.9.20 起按「按住时长」分档；第 20/23 轮斜坡放缓；第 32 轮废弃分档。
     * 旧模型实测（QBoxSeek 逐 tick 日志）：档间 BLEND 窗结束后的固定步长段速率反而低于窗内峰值
     * （如 14s 档窗内爬到 ~165x，窗外固定 100s/700ms≈143x），形成「高速段中夹低速段」的碎裂手感。
     *
     * 新模型：单一连续速率曲线，每 tick 位移 = 曲线定积分差值 cumAt(u2)−cumAt(u1)，中途无重置、无档位、无台阶。
     * 前 3 秒细调段保持不变（±1s @130ms，GS-42）；3 秒处曲线速率 R0=1000/130≈7.7x，与细调档标称速率连续衔接。
     * 第 36 轮重写（用户拍板）：**ramp 曲线与片长解耦——所有片长共用同一条基础倍率曲线
     * f(t)=R0+(BASE_MAX−R0)(1−e^(−(t−3s)/τ))（渐近 64x），片长只决定「截顶」位置**
     * （旧 B 模型 rate 随 cap 整体缩放，短片整条曲线被压扁，即用户抱怨的成因）。
     * cap = clamp(片长/60s, 8x, 60x)：设计目标「约 1 分钟按住可穿越全长」；
     * 下限 8x 不得低于细调段速率 R0（否则曲线断裂）；上限 60x = 1 小时片 60s 整穿越，
     * 再高时单 tick 位移 ≥7.2s，盲操过冲风险大于收益。
     * 片长由 setDuration 每 tick 注入，无片长时按 8x 兜底。快退与快进同一曲线对称（方向只取符号）。
     * 遥控器 FF/REW 媒体键与方向键共用本曲线。
     */
    private static final long FINE_MS = 3_000;      // 细调段时长（不变）
    private static final long FINE_STEP = 1_000;    // 细调步长（不变）
    private static final long FINE_TICK = 130;      // 细调节拍（不变）
    private static final long COARSE_TICK = 120;    // 粗调段最小节拍（防过密 seek）
    private static final double R0 = FINE_STEP / (double) FINE_TICK;   // ≈7.69x，3s 处连续
    private static final double TAU = 2_500.0;      // 爬升时间常数：t=5.5s 达 63% 爬升，t=9s 达 91%
    private static final double BASE_MAX = 64.0;    // 统一基础曲线渐近顶速：所有片长同一条 ramp
    private static final double CAP_MIN = 8.0;      // 片长封顶下限：不得低于 R0（保持连续）
    private static final double CAP_MAX = 60.0;     // 片长封顶上限：1 小时片约 1 分钟穿越

    private long durationMs;

    public void setDuration(long ms) {
        durationMs = Math.max(0, ms);
    }

    /** 片长封顶：纯片长函数，只截顶、不改变 ramp 形状。 */
    private double cap() {
        if (durationMs <= 0) return CAP_MIN;
        return Math.max(CAP_MIN, Math.min(CAP_MAX, durationMs / 60_000.0));
    }

    /** 统一 ramp 爬到本片 cap 的时点 u*（ms，u=elapsed−3000）；cap≥BASE_MAX 时不发生截顶。 */
    private double uStar(double cap) {
        double f = (cap - R0) / (BASE_MAX - R0);
        if (f >= 1.0) return Double.POSITIVE_INFINITY;
        return -TAU * Math.log(1.0 - f);
    }

    /** 基础曲线（渐近 BASE_MAX）累计位移的不定积分 G(u)=64u+(BASE_MAX−R0)·τ·e^(−u/τ)−(BASE_MAX−R0)·τ；G(0)=0。 */
    private double baseCum(double u) {
        return R0 * u + (BASE_MAX - R0) * (u + TAU * Math.exp(-u / TAU)) - (BASE_MAX - R0) * TAU;
    }

    /** 粗调段累计位移：u≤u* 走统一曲线积分，u>u* 后按 cap 线性延伸（截顶段）。 */
    private double cumAt(long u) {
        double cap = cap();
        double us = uStar(cap);
        return u <= us ? baseCum(u) : baseCum(us) + cap * (u - us);
    }

    private double rateAt(long u) {
        double r = R0 + (BASE_MAX - R0) * (1 - Math.exp(-u / TAU));
        return Math.min(r, cap());
    }

    private void seek(KeyEvent event, boolean forward) {
        long now = SystemClock.uptimeMillis();
        if (event.getRepeatCount() == 0) {
            // 按下首 tick 即 ±1s（GS-42 细调起步，不动）。
            holdStartTick = now;
            lastSeekTick = now;
            holdTime += forward ? 1_000 : -1_000;
            if (com.fongmi.android.tv.BuildConfig.DEBUG) {
                android.util.Log.d("QBoxSeek", "holdStart dur=" + durationMs + " cap=" + String.format(java.util.Locale.ROOT, "%.1f", cap()));
            }
            listener.onSeeking(holdTime);
            return;
        }
        long elapsed = now - holdStartTick;
        long step;
        if (elapsed < FINE_MS) {
            // 细调段：固定 ±1s 步长 + 130ms 节拍，与第 23 轮完全一致。
            if (now - lastSeekTick < FINE_TICK) return;
            lastSeekTick = now;
            step = FINE_STEP;
        } else {
            if (now - lastSeekTick < COARSE_TICK) return;
            long prevU = Math.max(0, lastSeekTick - holdStartTick - FINE_MS);
            long curU = elapsed - FINE_MS;
            lastSeekTick = now;
            step = Math.max(1, (long) (cumAt(curU) - cumAt(prevU)));
        }
        holdTime += forward ? step : -step;
        if (com.fongmi.android.tv.BuildConfig.DEBUG) {
            long u = Math.max(0, elapsed - FINE_MS);
            android.util.Log.d("QBoxSeek", "tick elapsed=" + elapsed
                    + " rate=" + String.format(java.util.Locale.ROOT, "%.2f", elapsed < FINE_MS ? R0 : rateAt(u))
                    + " step=" + step + " cum=" + holdTime + " dir=" + (forward ? "FF" : "REW"));
        }
        listener.onSeeking(holdTime);
    }

    /** 遥控器 FF/REW 由 PlaybackService 转发（媒体键被系统路由给 MediaSession，Activity 收不到 DOWN），与方向键共用同一斜坡。 */
    public void onMediaKey(KeyEvent event, boolean forward) {
        if (KeyUtil.isActionDown(event)) seek(event, forward);
        else if (KeyUtil.isActionUp(event)) App.post(() -> listener.onSeekEnd(holdTime), 250);
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
