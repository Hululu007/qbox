package com.fongmi.android.tv.ui.base;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.utils.Util;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import me.jessyan.autosize.AutoSizeCompat;

public abstract class BaseActivity extends AppCompatActivity {

    protected abstract ViewBinding getBinding();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getBinding().getRoot());
        EventBus.getDefault().register(this);
        initView(savedInstanceState);
        Util.hideSystemUI(this);
        setBackCallback();
        initEvent();
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        if (!showBackdrop()) return;
        View backdrop = new View(this);
        backdrop.setBackgroundResource(R.drawable.shape_backdrop_premium);
        backdrop.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        ((ViewGroup) findViewById(android.R.id.content)).addView(backdrop, 0, new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
    }

    protected FragmentActivity getActivity() {
        return this;
    }

    protected boolean showBackdrop() {
        return true;
    }

    protected void initView(Bundle savedInstanceState) {
    }

    protected void initEvent() {
    }

    protected boolean isVisible(View view) {
        return view.getVisibility() == View.VISIBLE;
    }

    protected boolean isGone(View view) {
        return view.getVisibility() == View.GONE;
    }

    protected void notifyItemChanged(RecyclerView view, RecyclerView.Adapter<?> adapter) {
        view.post(() -> adapter.notifyDataSetChanged());
    }

    private void setBackCallback() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                onBackInvoked();
            }
        });
    }

    /**
     * 万能逃生键：任意界面长按返回（连发 3 次以上）直接把 App 退出到电视桌面。
     * 用于固件待机恢复后黑屏、焦点丢失等「怎么按都退不出去」的极端状态。
     */
    @Override
    public boolean dispatchKeyEvent(android.view.KeyEvent event) {
        if (event.getKeyCode() == android.view.KeyEvent.KEYCODE_BACK && event.getRepeatCount() >= 3) {
            com.fongmi.android.tv.App.exitApp();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    private Resources hackResources(Resources resources) {
        try {
            AutoSizeCompat.autoConvertDensityOfGlobal(resources);
            return resources;
        } catch (Exception ignored) {
            return resources;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSubscribe(Object o) {
    }

    @Override
    public Resources getResources() {
        return hackResources(super.getResources());
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Util.hideSystemUI(this);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus) return;
        Util.hideSystemUI(this);
        // 从别的页面/系统弹窗回来时，焦点经常被系统直接回收（整棵树没有任何 focused 节点），
        // 表现就是「遥控器按什么都没反应，只能强退」。这里做全局兜底：谁都没焦点就找第一个可聚焦控件。
        View root = findViewById(android.R.id.content);
        if (root != null) root.postDelayed(() -> {
            if (getCurrentFocus() != null) return;
            View first = findFocusable(root);
            if (first != null) first.requestFocus();
        }, 200);
    }

    private View findFocusable(View view) {
        return com.fongmi.android.tv.utils.ViewUtil.findFirstFocusable(view);
    }

    protected void onBackInvoked() {
        finish();
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }
}
