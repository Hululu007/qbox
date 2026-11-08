package com.fongmi.android.tv.server;

import android.util.Log;

import com.fongmi.android.tv.service.PlaybackService;
import com.fongmi.android.tv.utils.Task;
import com.github.catvod.Proxy;
import com.github.catvod.utils.Util;

public class Server {

    private static final String TAG = "QBoxServer";

    private volatile PlaybackService service;
    private volatile Nano nano;

    private static class Loader {
        static volatile Server INSTANCE = new Server();
    }

    public static Server get() {
        return Loader.INSTANCE;
    }

    public PlaybackService getService() {
        return service;
    }

    public void setService(PlaybackService service) {
        this.service = service;
    }

    public String getAddress() {
        return getAddress(false);
    }

    public String getAddress(int tab) {
        return getAddress(false) + "?tab=" + tab;
    }

    public String getAddress(String path) {
        return getAddress(true) + path;
    }

    public String getAddress(boolean local) {
        return "http://" + (local ? "127.0.0.1" : Util.getIp()) + ":" + Proxy.getPort();
    }

    public synchronized void start() {
        if (nano != null) return;
        for (int i = 9978; i < 9999; i++) {
            try {
                nano = new Nano(i);
                nano.start(500);
                Proxy.set(i);
                Log.i(TAG, "server started on port " + i);
                return;
            } catch (Throwable e) {
                nano = null;
                Log.w(TAG, "port " + i + " unavailable: " + e.getMessage());
            }
        }
        // 9978-9998 全被占用时兜底：让系统分配空闲端口，保证推送页与代理服务始终可用。
        try {
            nano = new Nano(0);
            nano.start(500);
            Proxy.set(nano.getListeningPort());
            Log.i(TAG, "server started on system-assigned port " + nano.getListeningPort());
        } catch (Throwable e) {
            nano = null;
            Log.e(TAG, "server start failed", e);
        }
    }

    public void stop() {
        Task.execute(() -> {
            if (nano != null) nano.stop();
            service = null;
            nano = null;
        });
    }
}
