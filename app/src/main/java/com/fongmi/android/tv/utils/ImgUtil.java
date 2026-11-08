package com.fongmi.android.tv.utils;

import static android.widget.ImageView.ScaleType.FIT_CENTER;
import static android.widget.ImageView.ScaleType.MATRIX;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.impl.CustomTarget;
import com.github.catvod.utils.Json;
import com.google.common.net.HttpHeaders;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jahirfiquitiva.libs.textdrawable.TextDrawable;

public class ImgUtil {

    private static final Set<String> failed = Collections.synchronizedSet(new HashSet<>());

    public static void logo(ImageView view) {
        try {
            String logo = VodConfig.get().getConfig().getLogo();
            // 配置里没写 logo（或加载失败）时用 QBox 自己的 logo；不裁圆，立方体要保持完整。
            view.setScaleType(FIT_CENTER);
            if (TextUtils.isEmpty(logo)) {
                view.setImageResource(R.drawable.ic_logo);
                return;
            }
            Glide.with(view).load(UrlUtil.convert(logo)).override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL).error(R.drawable.ic_logo).into(view);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static void load(String url, CustomTarget<Bitmap> target) {
        try {
            Glide.with(App.get()).asBitmap().load(getUrl(url)).override(ResUtil.dp2px(96), ResUtil.dp2px(96)).error(R.drawable.artwork).into(target);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static void load(Context context, String url, CustomTarget<Drawable> target) {
        try {
            Glide.with(context).load(getUrl(url)).override(ResUtil.getScreenWidth(), ResUtil.getScreenHeight()).error(R.drawable.artwork).into(target);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static void load(String text, String url, ImageView view) {
        load(text, url, view, true);
    }

    public static void load(String text, String url, ImageView view, boolean vod) {
        view.setScaleType(vod ? MATRIX : FIT_CENTER);
        if (!vod) view.setVisibility(TextUtils.isEmpty(url) ? View.GONE : View.VISIBLE);
        if (TextUtils.isEmpty(url) || failed.contains(url)) {
            // 占位图/失败兜底用居中适配，不能套顶对齐矩阵（会把文字图标拉爆）。
            view.setScaleType(FIT_CENTER);
            view.setImageDrawable(getTextDrawable(text, vod));
        } else try {
            // 不要用 centerCrop()：那会把"居中裁剪"直接烤进解码结果，顶对齐矩阵就失效了。
            // dontTransform() 让 Glide 只按 View 尺寸解码、不做任何几何变换，
            // 原始宽高比完整保留，铺满与裁剪全部交给后面的顶对齐矩阵（避免二次缩放变糊）。
            Glide.with(view).load(getUrl(url)).dontTransform().listener(getListener(text, url, view, vod)).into(view);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * 海报顶对齐裁剪（top-start crop）。
     *
     * <p>卡片是 3:4，海报图多数是 2:3，CENTER_CROP 会在上下各裁掉约 5%——
     * 海报标题文字正好在顶部，被一条水平线切掉半截，焦点放大后尤其扎眼
     * （用户看起来就是"海报上部分被 tab 切了"）。改成矩阵裁剪：
     * <ul>
     *   <li>顶部永远从海报第 0 行开始显示，标题文字完整；</li>
     *   <li>垂直方向多出的部分全部裁在底部（那里通常是出品方/平台角标，无信息量）；</li>
     *   <li>水平方向如有多余则左右居中裁（宽版海报/横图）。尺寸不足的方向照常放大铺满。</li>
     * </ul>
     */
    private static void applyTopCrop(ImageView view) {
        Drawable d = view.getDrawable();
        if (d == null || view.getWidth() == 0 || view.getHeight() == 0) return;
        int dw = d.getIntrinsicWidth(), dh = d.getIntrinsicHeight();
        if (dw <= 0 || dh <= 0) return;
        float scale = Math.max(view.getWidth() / (float) dw, view.getHeight() / (float) dh);
        Matrix matrix = new Matrix();
        matrix.setScale(scale, scale);
        // 垂直顶对齐；水平居中（结果为负数时表示两侧均匀裁掉）。
        matrix.postTranslate((view.getWidth() - dw * scale) / 2f, 0);
        view.setImageMatrix(matrix);
        view.invalidate();
    }

    /** 图到位时视图可能还没量完尺寸（首帧/缓存命中），等它有尺寸了再算矩阵，只等一次。 */
    private static void postTopCrop(ImageView view) {
        if (view.getWidth() > 0 && view.getHeight() > 0) {
            applyTopCrop(view);
            return;
        }
        view.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                applyTopCrop(view);
            }
        });
    }

    public static Object getUrl(String url) {
        String param = null;
        url = UrlUtil.convert(url);
        url = unwrapProxy(url);
        if (url.startsWith("data:")) return url;
        LazyHeaders.Builder builder = new LazyHeaders.Builder();
        if (url.contains("@Headers=")) addHeader(builder, param = url.split("@Headers=")[1].split("@")[0]);
        if (url.contains("@Cookie=")) builder.addHeader(HttpHeaders.COOKIE, param = url.split("@Cookie=")[1].split("@")[0]);
        if (url.contains("@Referer=")) builder.addHeader(HttpHeaders.REFERER, param = url.split("@Referer=")[1].split("@")[0]);
        if (url.contains("@User-Agent=")) builder.addHeader(HttpHeaders.USER_AGENT, param = url.split("@User-Agent=")[1].split("@")[0]);
        url = param == null ? url : url.split("@")[0];
        return TextUtils.isEmpty(url) ? null : new GlideUrl(url, builder.build());
    }

    /**
     * 部分源的图片走百度图床代理，路径被拼成 gimg/app=...&src=原图，代理地址在电视上只会返回 1px 占位图。
     * 这里把 src 里的原图地址取出来直接用，海报、背景图都能正常显示。
     */
    private static String unwrapProxy(String url) {
        if (url == null || !url.contains(".baidu.com/gimg")) return url;
        int start = url.indexOf("src=");
        if (start < 0) return url;
        String src = url.substring(start + 4);
        int end = src.indexOf('&');
        if (end >= 0) src = src.substring(0, end);
        if (src.isEmpty()) return url;
        if (!src.startsWith("http")) src = "https://" + src;
        return src;
    }

    private static void addHeader(LazyHeaders.Builder builder, String header) {
        Map<String, String> map = Json.toMap(Json.parse(header));
        for (Map.Entry<String, String> entry : map.entrySet()) builder.addHeader(UrlUtil.fixHeader(entry.getKey()), entry.getValue());
    }

    private static Drawable getTextDrawable(String text, boolean vod) {
        TextDrawable.Builder builder = new TextDrawable.Builder();
        text = TextUtils.isEmpty(text) ? "！" : text.substring(0, 1);
        if (vod) builder.buildRect(text, ColorGenerator.get400(text));
        return builder.buildRoundRect(text, ColorGenerator.get400(text), ResUtil.dp2px(4));
    }

    private static RequestListener<Drawable> getListener(String text, String url, ImageView view, boolean vod) {
        return new RequestListener<>() {
            @Override
            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                if (vod) postTopCrop(view);
                return false;
            }

            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, @NonNull Target<Drawable> target, boolean isFirstResource) {
                view.setScaleType(FIT_CENTER);
                view.setImageDrawable(getTextDrawable(text, vod));
                failed.add(url);
                return true;
            }
        };
    }
}
