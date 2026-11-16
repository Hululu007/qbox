package com.fongmi.android.tv.utils;

/**
 * 待机 / 熄屏安全阀：电视熄屏后长时间没人看，再亮屏时直接把 App 退出到电视桌面，
 * 避免某些固件切回前台后 Surface 已失效、画面全黑又退不出去（用户明确要求「别让电视卡在我们的产品上」）。
 *
 * 记录熄屏时刻用 wall clock（deep sleep 期间 elapsedRealtime 不走，不能用来算待机时长）。
 */
public final class ScreenGuard {

    /** 熄屏超过 30 分钟，亮屏即退出 App。 */
    public static final long DEFAULT_SLEEP_EXIT_MS = 30 * 60 * 1000L;

    private static volatile long sleepAt = 0;
    private static volatile boolean exitRequested = false;

    private ScreenGuard() {
    }

    public static void onScreenOff() {
        if (sleepAt == 0) sleepAt = System.currentTimeMillis();
    }

    /** @return true 表示这次亮屏应该直接退出 App */
    public static boolean onScreenOn() {
        long at = sleepAt;
        sleepAt = 0;
        if (at <= 0) return false;
        if (System.currentTimeMillis() - at < DEFAULT_SLEEP_EXIT_MS) return false;
        exitRequested = true;
        return true;
    }

    /** DEBUG 用：模拟「熄屏了 ms 毫秒后亮屏」，验证退出逻辑。@return true 表示应退出 App */
    public static boolean simulateSleep(long ms) {
        sleepAt = System.currentTimeMillis() - Math.max(0, ms);
        return onScreenOn();
    }

    /** 取一次退出请求并清空，避免重复触发。 */
    public static boolean consumeExit() {
        boolean value = exitRequested;
        exitRequested = false;
        return value;
    }
}
