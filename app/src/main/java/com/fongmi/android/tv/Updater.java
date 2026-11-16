package com.fongmi.android.tv;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.text.TextUtils;
import android.view.View;

import androidx.fragment.app.FragmentActivity;

import com.fongmi.android.tv.impl.UpdateListener;
import com.fongmi.android.tv.receiver.UpdateReceiver;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.ui.dialog.UpdateDialog;
import com.fongmi.android.tv.utils.Download;
import com.fongmi.android.tv.utils.FileUtil;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Task;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Path;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Response;

/**
 * QBox 在线更新：指向 https://github.com/zqs1qiwan/qbox 的 latest Release。
 * 版本号取 tag（v1.9.11），按 ABI 选 QBox-<version>-<abi>.apk，下载完交给系统安装器。
 */
public class Updater implements Download.Callback, UpdateListener {

    private static final String REPO = "zqs1qiwan/qbox";
    private static final String API = "https://api.github.com/repos/" + REPO + "/releases/latest";
    private static final String LATEST = "https://github.com/" + REPO + "/releases/latest";
    private static final String TAG = "qbox-update";

    private Download download;
    private UpdateDialog dialog;
    private String url;
    private boolean force;

    public static Updater create() {
        return new Updater();
    }

    public Updater force() {
        this.force = true;
        return this;
    }

    private File getFile() {
        return Path.cache("update.apk");
    }

    private String getAbi() {
        return BuildConfig.FLAVOR_abi.replace('_', '-');
    }

    public void start(FragmentActivity activity) {
        if (activity == null || activity.isFinishing()) return;
        // 调试版不自动弹更新，避免开发时被 latest 反复打断；设置页手动检查仍然可用。
        if (!force && (BuildConfig.DEBUG || !Setting.getUpdate())) return;
        if (force) Notify.show(R.string.update_check);
        Task.submit(() -> check(activity));
    }

    private void check(FragmentActivity activity) {
        String version = null;
        String desc = null;
        String asset = null;
        try {
            JSONObject object = new JSONObject(OkHttp.string(API));
            version = object.optString("tag_name");
            desc = object.optString("body");
            asset = findAsset(object.optJSONArray("assets"), version);
        } catch (Throwable e) {
            // API 可能被限流或直连失败，退回 releases/latest 的 302 地址取 tag。
            try {
                version = resolveLatestTag();
            } catch (Throwable ignored) {
            }
        }
        if (TextUtils.isEmpty(version) || !isNewer(version)) {
            if (force) App.post(() -> Notify.show(R.string.update_latest));
            return;
        }
        url = TextUtils.isEmpty(asset) ? buildUrl(version) : asset;
        String name = version;
        String body = desc;
        App.post(() -> show(activity, name, body));
    }

    /** 跟随 releases/latest 的跳转，从最终地址里取 tag。 */
    private String resolveLatestTag() throws Exception {
        try (Response res = OkHttp.newCall(LATEST, TAG).execute()) {
            String real = res.request().url().toString();
            int index = real.indexOf("/tag/");
            return index < 0 ? null : real.substring(index + 5);
        }
    }

    private String findAsset(JSONArray assets, String version) {
        if (assets == null) return null;
        String abi = getAbi();
        String wanted = "QBox-" + cleanTag(version) + "-" + abi + ".apk";
        String fallback = null;
        for (int i = 0; i < assets.length(); i++) {
            JSONObject item = assets.optJSONObject(i);
            if (item == null) continue;
            String name = item.optString("name");
            if (!name.endsWith(".apk")) continue;
            String browser = item.optString("browser_download_url");
            if (name.equalsIgnoreCase(wanted)) return browser;
            if (fallback == null && name.contains(abi)) fallback = browser;
        }
        return fallback;
    }

    private String buildUrl(String version) {
        return "https://github.com/" + REPO + "/releases/download/" + version + "/QBox-" + cleanTag(version) + "-" + getAbi() + ".apk";
    }

    private String cleanTag(String tag) {
        return tag == null ? "" : tag.trim().replaceFirst("^[vV]", "");
    }

    private boolean isNewer(String tag) {
        List<Integer> remote = parse(cleanTag(tag));
        List<Integer> local = parse(BuildConfig.VERSION_NAME);
        int size = Math.max(remote.size(), local.size());
        for (int i = 0; i < size; i++) {
            int a = i < remote.size() ? remote.get(i) : 0;
            int b = i < local.size() ? local.get(i) : 0;
            if (a != b) return a > b;
        }
        return false;
    }

    private List<Integer> parse(String version) {
        List<Integer> list = new ArrayList<>();
        for (String part : version.split("\\.")) {
            try {
                list.add(Integer.parseInt(part.replaceAll("\\D", "")));
            } catch (Exception e) {
                list.add(0);
            }
        }
        return list;
    }

    private void show(FragmentActivity activity, String version, String desc) {
        if (activity == null || activity.isFinishing()) return;
        dismiss();
        String text = clean(desc);
        dialog = UpdateDialog.create()
                .title(ResUtil.getString(R.string.update_version, version))
                .desc(TextUtils.isEmpty(text) ? ResUtil.getString(R.string.update_found) : text)
                .listener(this)
                .show(activity);
    }

    /** Release 说明是 Markdown；更新弹窗只保留适合最终用户阅读的短句和要点。 */
    private String clean(String desc) {
        if (TextUtils.isEmpty(desc)) return "";
        StringBuilder sb = new StringBuilder();
        boolean fenced = false;
        boolean truncated = false;
        for (String raw : desc.split("\n")) {
            String line = raw.trim();
            if (line.startsWith("```")) {
                fenced = !fenced;
                continue;
            }
            if (fenced || line.isEmpty() || line.equals("---") || line.startsWith("|")) continue;
            // 版本标题由弹窗自己显示；表格/下载/校验信息不属于「更新内容」。
            if (line.matches("^#*\\s*QBox\\s+v?\\d.*")) continue;
            if (line.matches("^(本次更新|更新内容|更新说明)[：:]?$")) continue;
            // 安装/下载说明属于 Release 页面内容，弹窗只保留「这次改了什么」。
            if (line.matches("^(升级方式|安装方式|下载方式|安装说明|下载地址)[：:]?.*")) break;
            if (line.contains("github.com/") || line.matches("^(SHA-?256|下载地址|发布日期).*")) continue;
            boolean bullet = line.matches("^[-*+]\\s+.*");
            line = line.replaceFirst("^[-*+]\\s+", "");
            line = line.replaceFirst("^#+\\s*", "");
            line = line.replaceAll("\\[([^\\]]+)\\]\\([^)]*\\)", "$1");
            line = line.replace("**", "").replace("__", "").replace("`", "").replace("~~", "");
            if (line.isEmpty()) continue;
            String formatted = bullet ? "• " + line : line;
            if (sb.length() + formatted.length() > 600) {
                truncated = true;
                break;
            }
            if (sb.length() > 0) sb.append('\n');
            sb.append(formatted);
        }
        return sb.toString().trim() + (truncated ? "…" : "");
    }

    @Override
    public void onConfirm(View view) {
        view.setEnabled(false);
        download = Download.create(url, getFile()).tag(TAG);
        download.start(this);
    }

    @Override
    public void onCancel(View view) {
        Setting.putUpdate(false);
        if (download != null) download.cancel();
        dismiss();
    }

    private void dismiss() {
        try {
            if (dialog != null) dialog.dismiss();
        } catch (Exception ignored) {
        }
    }

    @Override
    public void progress(int progress) {
        if (dialog != null) dialog.setProgress(progress);
    }

    @Override
    public void error(String msg) {
        Notify.show(ResUtil.getString(R.string.update_failed));
        dismiss();
    }

    @Override
    public void success(File file) {
        dismiss();
        install(file);
    }

    /**
     * 走 PackageInstaller 会话安装，不再依赖各 ROM 的系统安装器 Activity
     * （部分电视盒子的 InstallStart 会直接报「应用未安装」）。
     */
    private void install(File file) {
        try {
            PackageInstaller installer = App.get().getPackageManager().getPackageInstaller();
            PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            params.setAppPackageName(App.get().getPackageName());
            int id = installer.createSession(params);
            PackageInstaller.Session session = installer.openSession(id);
            try (FileInputStream in = new FileInputStream(file);
                 OutputStream out = session.openWrite("qbox", 0, file.length())) {
                byte[] buffer = new byte[65536];
                int read;
                while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
                session.fsync(out);
            }
            Intent intent = new Intent(App.get(), UpdateReceiver.class).setAction(UpdateReceiver.ACTION);
            // 系统要往这个 Intent 里补 EXTRA_STATUS / EXTRA_INTENT，Android 12+ 必须声明 MUTABLE。
            int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE;
            PendingIntent pending = PendingIntent.getBroadcast(App.get(), id, intent, flags);
            session.commit(pending.getIntentSender());
            session.close();
        } catch (Throwable e) {
            e.printStackTrace();
            // 极少数 ROM 的会话接口不可用，退回系统安装器。
            FileUtil.openFile(file);
        }
    }
}
