package com.fongmi.android.tv;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.HandlerCompat;

import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ScreenGuard;
import com.fongmi.hook.Hook;
import com.github.catvod.Init;
import com.google.gson.Gson;

public class App extends Application implements Application.ActivityLifecycleCallbacks {

    private static volatile App instance;

    private final Handler handler;
    private final Gson gson;
    private final long time;

    private Activity activity;
    private Hook hook;

    public App() {
        instance = this;
        gson = new Gson();
        time = System.currentTimeMillis();
        handler = HandlerCompat.createAsync(Looper.getMainLooper());
    }

    public static App get() {
        return instance;
    }

    public static Gson gson() {
        return get().gson;
    }

    public static long time() {
        return get().time;
    }

    public static Activity activity() {
        return get().activity;
    }

    public static void post(Runnable runnable) {
        get().handler.post(runnable);
    }

    public static void post(Runnable runnable, long delayMillis) {
        get().handler.removeCallbacks(runnable);
        if (delayMillis >= 0) get().handler.postDelayed(runnable, delayMillis);
    }

    public static void removeCallbacks(Runnable runnable) {
        get().handler.removeCallbacks(runnable);
    }

    public static void removeCallbacks(Runnable... runnable) {
        for (Runnable r : runnable) get().handler.removeCallbacks(r);
    }

    public void setHook(Hook hook) {
        this.hook = hook;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Init.set(base);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // 源站名 emoji 支持（用户拍板「源上 emoji 更好肉眼识别」）——盒子固件字体链
        // 渲染不出 emoji（实测 ▯），bundled NotoColorEmoji 随包内嵌；异步加载不阻塞启动。
        androidx.emoji2.text.EmojiCompat.init(new androidx.emoji2.bundled.BundledEmojiCompatConfig(this));
        Notify.createChannel();
        registerActivityLifecycleCallbacks(this);
        registerScreenGuard();
        // 本地 HTTP 服务（推送页/代理）进程启动即拉起：此前只在配置加载时启动，
        // 新装无配置的用户进推送页会拿到未启动的端口 -1（.130 实测复现）。幂等，bind 失败自带端口递增与系统分配兜底。
        Server.get().start();
    }

    /** 熄屏/亮屏安全阀：熄屏超过 30 分钟，亮屏后直接退出 App，避免固件恢复后黑屏又退不出去。 */
    private void registerScreenGuard() {
        android.content.IntentFilter filter = new android.content.IntentFilter();
        filter.addAction(android.content.Intent.ACTION_SCREEN_OFF);
        filter.addAction(android.content.Intent.ACTION_SCREEN_ON);
        registerReceiver(new android.content.BroadcastReceiver() {
            @Override
            public void onReceive(Context context, android.content.Intent intent) {
                if (intent == null) return;
                if (android.content.Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) ScreenGuard.onScreenOff();
                else if (android.content.Intent.ACTION_SCREEN_ON.equals(intent.getAction()) && ScreenGuard.onScreenOn()) post(App::exitApp);
            }
        }, filter);
        if (BuildConfig.DEBUG) androidx.core.content.ContextCompat.registerReceiver(this, new android.content.BroadcastReceiver() {
            @Override
            public void onReceive(Context context, android.content.Intent intent) {
                // adb shell am broadcast -a com.qbox.app.DEBUG_SIMULATE_WAKE --el ms 1800000
                if (ScreenGuard.simulateSleep(intent == null ? 0 : intent.getLongExtra("ms", 0))) post(App::exitApp);
            }
        }, new android.content.IntentFilter(DEBUG_SIMULATE_WAKE), androidx.core.content.ContextCompat.RECEIVER_EXPORTED);
    }

    public static final String DEBUG_SIMULATE_WAKE = "com.qbox.app.DEBUG_SIMULATE_WAKE";

    /** 退出整个 App：停掉后台播放/媒体服务并移除任务，回到电视桌面。 */
    public static void exitApp() {
        Activity activity = activity();
        // 没有前台 Activity 时保留退出标记，等 onActivityResumed 再处理。
        if (activity == null) return;
        // 清掉待处理的退出请求，避免下次启动时 onActivityResumed 又踢一次。
        ScreenGuard.consumeExit();
        try {
            activity.stopService(new android.content.Intent(activity, com.fongmi.android.tv.service.PlaybackService.class));
        } catch (Throwable ignored) {
        }
        try {
            activity.finishAndRemoveTask();
        } catch (Throwable ignored) {
        }
    }

    @Override
    public PackageManager getPackageManager() {
        return hook != null ? hook : getBaseContext().getPackageManager();
    }

    @Override
    public String getPackageName() {
        return hook != null ? hook.getPackageName() : getBaseContext().getPackageName();
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        if (activity != activity()) this.activity = activity;
        // 熄屏过久（>30 分钟）后亮屏：直接退出到电视桌面，避免黑屏状态退不出去。
        if (ScreenGuard.consumeExit()) {
            try {
                activity.stopService(new android.content.Intent(activity, com.fongmi.android.tv.service.PlaybackService.class));
            } catch (Throwable ignored) {
            }
            activity.finishAndRemoveTask();
        }
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        if (activity == activity()) this.activity = null;
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
    }
}
