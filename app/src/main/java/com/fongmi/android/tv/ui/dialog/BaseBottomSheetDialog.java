package com.fongmi.android.tv.ui.dialog;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import androidx.activity.ComponentDialog;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Util;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public abstract class BaseBottomSheetDialog extends BottomSheetDialogFragment {

    private final OnBackPressedCallback backCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            onBackInvoked();
        }
    };

    protected abstract ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container);

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(d -> setSheet(dialog));
        Window window = dialog.getWindow();
        if (window == null) return dialog;
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        if (Util.isFullscreen(getActivity())) window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return getBinding(inflater, container).getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupBackDispatcher();
        initView();
        initEvent();
    }

    protected void initView() {
    }

    protected void initEvent() {
    }

    protected void onBackInvoked() {
    }

    protected final void setBackCallbackEnabled(boolean enabled) {
        backCallback.setEnabled(enabled);
    }

    protected int getMaxHeight() {
        return 0;
    }

    private void setSheet(BottomSheetDialog dialog) {
        FrameLayout sheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (sheet == null) return;
        if (Util.isLeanback()) {
            // TV 面板化：高度由内容决定（上限 85% 屏高，超出内部滚动），不走手机的固定半屏高度。
            setBehavior(sheet);
            panelize(dialog, sheet);
            return;
        }
        setHeight(sheet, getMaxHeight());
        setBehavior(sheet);
    }

    /**
     * TV 端把底部弹层面板化：居中、固定宽 560dp、四角圆角 28dp，与全站面板对话框
     * 同一套外观（surface 色、圆角、居中、淡入淡出），消灭「播放中菜单另起一套 UI」。
     * 仅 leanback 生效；手机端保留底部弹层惯用形态。BACK 关闭行为不受影响。
     * 注意必须同时清掉 BottomSheetBehavior 的 maxWidth：Material 的 bottomSheetStyle
     * 会给 sheet 一个内容相关宽度（真机实测 sheet 只占窗口右侧 ~400dp），
     * 不清掉的话窗口虽是 560dp 居中，面板本身却偏右且宽度随内容跳。
     */
    private void panelize(BottomSheetDialog dialog, FrameLayout sheet) {
        sheet.setBackgroundResource(R.drawable.shape_panel);
        int width = ResUtil.dp2px(560);
        BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(sheet);
        behavior.setMaxWidth(width);
        behavior.setMaxHeight((int) (ResUtil.getScreenHeight() * 0.85f));
        // 本机会上报假的左侧系统 inset（320px），Material 会把它转成 sheet 的 marginLeft，
        // 面板偏右且右缘被裁（实测 sheet=[720..1520]，窗口=[400..1520]）。
        // 吃掉 sheet 的 inset 分发；布局监听器常驻、每次布局强制归零 margin（inset 何时下发不确定，一次性清不掉）。
        ViewCompat.setOnApplyWindowInsetsListener(sheet, (v, insets) -> WindowInsetsCompat.CONSUMED);
        fixSheetFrame(sheet);
        sheet.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                fixSheetFrame(sheet);
                centerSheet(sheet);
                if (sheet.getLeft() > 0) android.util.Log.e("QBoxSheet", "sheet pushed right: left=" + sheet.getLeft() + " marginLeft=" + ((ViewGroup.MarginLayoutParams) sheet.getLayoutParams()).leftMargin);
            }
        });
        Window window = dialog.getWindow();
        if (window == null) return;
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.width = width;
        lp.gravity = Gravity.CENTER;
        // 与主窗口一致：部分电视盒子上报假的刘海安全区（实测左侧 320px），
        // 不设的话 Material 会把安全区转成 sheet 的 marginLeft，面板偏右且右侧被裁掉。
        lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        window.setAttributes(lp);
        window.setWindowAnimations(android.R.style.Animation_Dialog);
    }

    /**
     * BottomSheetBehavior 永远把 sheet 贴在窗口底部；窗口保持全高，用 translationY 把 sheet
     * 平移到垂直居中（每次布局重算，behavior 重排后会被重置，所以常驻监听里反复应用）。
     */
    private void centerSheet(FrameLayout sheet) {
        View parent = (View) sheet.getParent();
        if (parent == null || parent.getHeight() == 0 || sheet.getHeight() == 0) return;
        float ty = Math.max(0, (parent.getHeight() - sheet.getHeight()) / 2f) - sheet.getTop();
        if (Math.abs(sheet.getTranslationY() - ty) > 1f) sheet.setTranslationY(ty);
    }

    private void fixSheetFrame(FrameLayout sheet) {
        ViewGroup.LayoutParams sp = sheet.getLayoutParams();
        sp.width = ViewGroup.LayoutParams.MATCH_PARENT;
        if (sp instanceof ViewGroup.MarginLayoutParams) ((ViewGroup.MarginLayoutParams) sp).setMargins(0, 0, 0, 0);
        sheet.setLayoutParams(sp);
    }

    private void setHeight(FrameLayout sheet, int maxHeight) {
        if (maxHeight <= 0) return;
        ViewGroup.LayoutParams params = sheet.getLayoutParams();
        params.height = maxHeight;
        sheet.setLayoutParams(params);
    }

    private void setBehavior(FrameLayout sheet) {
        BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(sheet);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        behavior.setSkipCollapsed(true);
    }

    private void setupBackDispatcher() {
        if (requireDialog() instanceof ComponentDialog dialog) dialog.getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), backCallback);
    }

    @Override
    public void onDestroyView() {
        backCallback.setEnabled(false);
        super.onDestroyView();
    }
}
