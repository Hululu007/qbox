package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.palette.graphics.Palette;

import com.fongmi.android.tv.databinding.ViewWallBinding;
import com.fongmi.android.tv.event.ConfigEvent;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.utils.FileUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;

/**
 * 全局氛围底：统一深色渐变（shape_wall_premium），并按壁纸缓存提取主题色。
 * 旧的「按 wall/wallType 加载内置壁纸 / GIF / 视频」链路已随 QBox 简约化移除。
 */
public class CustomWallView extends FrameLayout implements DefaultLifecycleObserver {

    private static final int[] WALL_COLORS = {0, 0xFF40C090, 0xFF4870E0, 0xFF48B0C0, 0xFF404040};

    private ViewWallBinding binding;

    public CustomWallView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (isInEditMode()) return;
        binding = ViewWallBinding.inflate(LayoutInflater.from(getContext()), this, true);
        ((ComponentActivity) getContext()).getLifecycle().addObserver(this);
        refresh();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onConfigEvent(ConfigEvent event) {
        if (event.type() == ConfigEvent.Type.WALL) refresh();
    }

    private void refresh() {
        load();
        theme();
    }

    private void load() {
        // QBox: 统一使用深色渐变底，不再加载配置源的花哨壁纸（Infuse/Netflix 式简约）。
        binding.image.setImageResource(com.fongmi.android.tv.R.drawable.shape_wall_premium);
    }

    private void theme() {
        int newColor = getWallColor();
        int oldColor = Setting.getWallColor();
        if (newColor == oldColor) return;
        Setting.putWallColor(newColor);
        if (Setting.getThemeColor() == 0) RefreshEvent.theme();
    }

    private int getWallColor() {
        int wall = Setting.getWall();
        int type = Setting.getWallType();
        if (isBuiltIn(wall, type)) return WALL_COLORS[wall];
        File file = FileUtil.getWallCache();
        return file.exists() ? paletteColor(file) : WALL_COLORS[1];
    }

    private int paletteColor(File file) {
        Bitmap bitmap = decodeBitmap(file);
        if (bitmap == null) return WALL_COLORS[1];
        Palette palette = Palette.from(bitmap).maximumColorCount(8).generate();
        bitmap.recycle();
        return swatchColor(palette);
    }

    private Bitmap decodeBitmap(File file) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = 8;
        return BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
    }

    private int swatchColor(Palette palette) {
        Palette.Swatch swatch = palette.getVibrantSwatch();
        if (swatch == null) swatch = palette.getDominantSwatch();
        return swatch != null ? swatch.getRgb() : WALL_COLORS[1];
    }

    private boolean isBuiltIn(int wall, int type) {
        return type == 0 && wall > 0 && wall < WALL_COLORS.length;
    }

    @Override
    public void onCreate(@NonNull LifecycleOwner owner) {
        EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        EventBus.getDefault().unregister(this);
        binding = null;
    }
}