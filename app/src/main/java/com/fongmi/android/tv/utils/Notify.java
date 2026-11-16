package com.fongmi.android.tv.utils;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.content.Context;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.ViewProgressBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class Notify {

    public static final String DEFAULT = "default";
    public static final int ID = 9527;
    private AlertDialog mDialog;
    private View mOverlay;
    private Runnable mHideOverlay;

    private static class Loader {
        static volatile Notify INSTANCE = new Notify();
    }

    private static Notify get() {
        return Loader.INSTANCE;
    }

    public static void createChannel() {
        NotificationManagerCompat notifyMgr = NotificationManagerCompat.from(App.get());
        notifyMgr.createNotificationChannel(new NotificationChannelCompat.Builder(DEFAULT, NotificationManagerCompat.IMPORTANCE_LOW).setName("TV").build());
    }

    public static String getError(int resId, Throwable e) {
        if (TextUtils.isEmpty(e.getMessage())) return ResUtil.getString(resId);
        return ResUtil.getString(resId) + "\n" + e.getMessage();
    }

    public static void show(Notification notification) {
        if (ContextCompat.checkSelfPermission(App.get(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        NotificationManagerCompat.from(App.get()).notify(ID, notification);
    }

    public static void show(int resId) {
        if (resId != 0) show(ResUtil.getString(resId));
    }

    public static void show(String text) {
        if (!TextUtils.isEmpty(text)) get().makeText(text);
    }

    public static void progress(Context context) {
        dismiss();
        get().create(context);
    }

    public static void dismiss() {
        try {
            if (get().mDialog != null) get().mDialog.dismiss();
        } catch (Exception ignored) {
        }
    }

    private void create(Context context) {
        ViewProgressBinding binding = ViewProgressBinding.inflate(LayoutInflater.from(context));
        mDialog = new MaterialAlertDialogBuilder(context).setView(binding.getRoot()).create();
        mDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        mDialog.show();
    }

    private void makeText(String text) {
        // 系统 Toast 不经 AppCompat，emoji 需显式过 EmojiCompat.process 才能用内嵌字体渲染
        // （实测换源 toast 站名 emoji 豆腐块）；字体未加载完时返回原文，不受影响。
        CharSequence cs = text;
        try {
            if (androidx.emoji2.text.EmojiCompat.get().getLoadState() == androidx.emoji2.text.EmojiCompat.LOAD_STATE_SUCCEEDED) cs = androidx.emoji2.text.EmojiCompat.get().process(text);
        } catch (Exception ignored) {
        }
        // 系统 Toast 在部分 TV 上会回退成安卓默认机器人图标；改成 Activity 内浮层，
        // 固定显示 QBox 自己的立方体 Q 标识，不依赖系统读取应用图标。
        Activity activity = App.activity();
        if (activity == null || activity.isFinishing()) return;
        ViewGroup content = activity.findViewById(android.R.id.content);
        if (!(content instanceof FrameLayout)) return;
        FrameLayout frame = (FrameLayout) content;
        if (mHideOverlay != null) App.removeCallbacks(mHideOverlay);
        if (mOverlay != null && mOverlay.getParent() instanceof ViewGroup) ((ViewGroup) mOverlay.getParent()).removeView(mOverlay);
        View view = LayoutInflater.from(activity).inflate(R.layout.view_toast, frame, false);        TextView contentText = view.findViewById(R.id.text);
        contentText.setText(cs);
        view.setClickable(false);
        view.setFocusable(false);
        view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        params.bottomMargin = ResUtil.dp2px(96);
        frame.addView(view, params);
        mOverlay = view;
        mHideOverlay = () -> {
            if (mOverlay == view) mOverlay = null;
            if (view.getParent() instanceof ViewGroup) ((ViewGroup) view.getParent()).removeView(view);
        };
        App.post(mHideOverlay, 2800);
    }
}
