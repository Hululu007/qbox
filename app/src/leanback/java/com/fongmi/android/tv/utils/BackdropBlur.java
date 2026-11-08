package com.fongmi.android.tv.utils;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 详情页氛围底图模糊（第 27 轮）。
 * 目标：背景只要色调与明暗氛围，不要看清内容；源图小、放大 4× 后压缩颗粒/像素块明显，
 * 用「多级降采样 + 小尺寸模糊 + 放大回 480×270 + 轻抖动」把粗糙抹平：
 *   480×270 → 72×40（降采样本身即强柔化，≤8px 的 DCT 压缩块直接被融掉）
 *   → 72×40 上 3 趟盒式模糊 r=2（近似高斯，消除降采样 cell 边界）
 *   → 双线性放大回 480×270（再经 ImageView 4× 上屏，全程平滑）
 *   → ±1 LSB 确定性抖动（消平滑渐变的色带 banding）。
 * 72×40 保留海报主色调与明暗分布（每格 ≈ 屏上 27px 的柔和色块，构图仍可感），不糊成均匀灰。
 * 全部在 ≤480×270 尺度一次性处理（Glide 后台线程 + 磁盘缓存），渲染线程与播放零开销。
 */
public class BackdropBlur extends BitmapTransformation {

    private static final String ID = "com.fongmi.android.tv.utils.BackdropBlur.v1";
    private static final int SMALL_W = 72;
    private static final int SMALL_H = 40;

    @Override
    protected Bitmap transform(@NonNull BitmapPool pool, @NonNull Bitmap toTransform, int outWidth, int outHeight) {
        // 实测（.214，A53）：480×270 全程约 110-150ms，Glide 后台线程一次性执行 + 磁盘缓存，渲染线程/播放零开销。
        Bitmap small = Bitmap.createScaledBitmap(toTransform, SMALL_W, SMALL_H, true);
        boxBlur3(small, 2);
        Bitmap up = Bitmap.createScaledBitmap(small, toTransform.getWidth(), toTransform.getHeight(), true);
        dither(up);
        return up;
    }

    /** 三趟盒式模糊（水平+垂直滑动窗口），近似高斯；r 为小尺寸图上的半径。 */
    private static void boxBlur3(Bitmap bmp, int r) {
        for (int i = 0; i < 3; i++) {
            boxBlurPass(bmp, r, true);
            boxBlurPass(bmp, r, false);
        }
    }

    private static void boxBlurPass(Bitmap bmp, int r, boolean horizontal) {
        int w = bmp.getWidth();
        int h = bmp.getHeight();
        int[] px = new int[w * h];
        bmp.getPixels(px, 0, w, 0, 0, w, h);
        int[] out = new int[w * h];
        int div = 2 * r + 1;
        if (horizontal) {
            for (int y = 0; y < h; y++) {
                int base = y * w;
                int sumR = 0, sumG = 0, sumB = 0;
                for (int x = -r; x <= r; x++) {
                    int c = px[base + clamp(x, 0, w - 1)];
                    sumR += (c >> 16) & 0xFF; sumG += (c >> 8) & 0xFF; sumB += c & 0xFF;
                }
                for (int x = 0; x < w; x++) {
                    out[base + x] = 0xFF000000 | ((sumR / div) << 16) | ((sumG / div) << 8) | (sumB / div);
                    int add = px[base + clamp(x + r + 1, 0, w - 1)];
                    int sub = px[base + clamp(x - r, 0, w - 1)];
                    sumR += ((add >> 16) & 0xFF) - ((sub >> 16) & 0xFF);
                    sumG += ((add >> 8) & 0xFF) - ((sub >> 8) & 0xFF);
                    sumB += (add & 0xFF) - (sub & 0xFF);
                }
            }
        } else {
            for (int x = 0; x < w; x++) {
                int sumR = 0, sumG = 0, sumB = 0;
                for (int y = -r; y <= r; y++) {
                    int c = px[clamp(y, 0, h - 1) * w + x];
                    sumR += (c >> 16) & 0xFF; sumG += (c >> 8) & 0xFF; sumB += c & 0xFF;
                }
                for (int y = 0; y < h; y++) {
                    out[y * w + x] = 0xFF000000 | ((sumR / div) << 16) | ((sumG / div) << 8) | (sumB / div);
                    int add = px[clamp(y + r + 1, 0, h - 1) * w + x];
                    int sub = px[clamp(y - r, 0, h - 1) * w + x];
                    sumR += ((add >> 16) & 0xFF) - ((sub >> 16) & 0xFF);
                    sumG += ((add >> 8) & 0xFF) - ((sub >> 8) & 0xFF);
                    sumB += (add & 0xFF) - (sub & 0xFF);
                }
            }
        }
        bmp.setPixels(out, 0, w, 0, 0, w, h);
    }

    /** 确定性 ±1 抖动：坐标 hash 出伪随机 ±1，打散平滑渐变的色带；不引入可见噪声。 */
    private static void dither(Bitmap bmp) {
        int w = bmp.getWidth();
        int h = bmp.getHeight();
        int[] px = new int[w * h];
        bmp.getPixels(px, 0, w, 0, 0, w, h);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int n = ((x * 73 + y * 149) * 2654435761L >>> 31) == 0 ? 1 : -1;
                int i = y * w + x;
                int c = px[i];
                int rr = clamp(((c >> 16) & 0xFF) + n, 0, 255);
                int gg = clamp(((c >> 8) & 0xFF) + n, 0, 255);
                int bb = clamp((c & 0xFF) + n, 0, 255);
                px[i] = 0xFF000000 | (rr << 16) | (gg << 8) | bb;
            }
        }
        bmp.setPixels(px, 0, w, 0, 0, w, h);
    }

    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : Math.min(v, hi);
    }

    @Override
    public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
        messageDigest.update(ID.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BackdropBlur;
    }

    @Override
    public int hashCode() {
        return ID.hashCode();
    }
}
