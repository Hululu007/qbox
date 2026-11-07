package com.fongmi.android.tv.ui.dialog;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Util;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public abstract class BaseAlertDialog extends DialogFragment {

    protected abstract ViewBinding getBinding();

    protected abstract MaterialAlertDialogBuilder getBuilder();

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = getBuilder().create();
        initView();
        initEvent();
        return dialog;
    }

    protected MaterialAlertDialogBuilder builder() {
        return new MaterialAlertDialogBuilder(requireActivity());
    }

    protected void initView() {
    }

    protected void initEvent() {
    }

    @Override
    public void onStart() {
        super.onStart();
        // 简介等不显式调 setWidth 的面板也统一 560dp（leanback 下 factor 被忽略）。
        if (Util.isLeanback()) setWidth(0);
    }

    /**
     * 面板宽度策略统一（菜单统一组件）：TV 端 Alert 面板默认固定 560dp，
     * 与 BottomSheet 面板（BaseBottomSheetDialog.panelize）同宽、同圆角、同动画；
     * 列表/内容本身就少的紧凑弹窗（isCompact，如来源选择）回退到按屏幕比例取宽。
     * cutout 标志与主窗口一致：部分电视盒子上报假刘海安全区（实测左侧 320px），
     * 不设则面板内容被向右推且右缘被裁。
     */
    /** 紧凑弹窗：不套 560dp 统一宽，按 factor 取屏幕比例（第 40 轮，来源选择回退紧凑形态）。 */
    protected boolean isCompact() {
        return false;
    }

    protected void setWidth(float factor) {
        if (getDialog() == null || getDialog().getWindow() == null) return;
        WindowManager.LayoutParams params = getDialog().getWindow().getAttributes();
        params.width = Util.isLeanback() && !isCompact() ? ResUtil.dp2px(560) : (int) (ResUtil.getScreenWidth() * factor);
        params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        getDialog().getWindow().setAttributes(params);
        if (!Util.isLeanback()) return;
        // Material 默认对话框背景自带 ~25dp 内缩（给阴影让位），可见面板会比窗口窄。
        // TV 面板不要阴影：窗底透明、面板背景(shape_panel)贴到 decorView，可见边界=窗口边界=560dp。
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getDialog().getWindow().getDecorView().setBackgroundResource(R.drawable.shape_panel);
    }
}
