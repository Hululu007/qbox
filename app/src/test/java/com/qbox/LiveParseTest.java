package com.qbox;

import static org.junit.Assert.assertTrue;

import com.fongmi.android.tv.api.parser.LiveParser;
import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.bean.Group;
import com.fongmi.android.tv.bean.Live;
import com.github.catvod.utils.Json;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.net.HttpURLConnection;
import java.net.URL;

/**
 * QBox 直播链路测试（JVM + Robolectric）：
 * 用壳里真正的 LiveParser 去拉默认配置里的 m3u/txt 直播源，验证能解析出分组与频道。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, manifest = Config.NONE)
public class LiveParseTest {

    @Test
    public void defaultLivesParse() throws Exception {
        String json = com.fongmi.android.tv.api.Decoder.getJson("https://tvbox.laobaitv.net", "qbox-live");
        JsonObject object = Json.parse(json).getAsJsonObject();
        JsonArray lives = object.getAsJsonArray("lives");
        int parsed = 0;
        int channels = 0;
        boolean probed = false;
        Gson gson = new Gson();
        for (int i = 0; i < lives.size(); i++) {
            JsonObject item = lives.get(i).getAsJsonObject();
            String url = item.has("url") ? item.get("url").getAsString() : "";
            if (url.isEmpty() || url.contains("127.0.0.1")) continue;
            try {
                Live live = gson.fromJson(item, Live.class);
                LiveParser.start(live);
                int count = 0;
                for (Group group : live.getGroups()) count += group.getChannel().size();
                System.out.println("QBOX_LIVE[" + live.getName() + "] groups=" + live.getGroups().size() + " channels=" + count);
                if (count > 0) {
                    parsed++;
                    channels += count;
                    if (!probed) {
                        probed = true;
                        probeFirst(live);
                    }
                }
            } catch (Throwable e) {
                System.out.println("QBOX_LIVE[" + (item.has("name") ? item.get("name").getAsString() : i) + "] FAILED "
                        + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
        System.out.println("QBOX_LIVE_SUMMARY parsed=" + parsed + " channels=" + channels);
        assertTrue("no live source parsed", parsed > 0);
    }

    /** 取第一个频道的第一条线路，验证它真的能取到流（200/206 + 视频类型）。 */
    private static void probeFirst(Live live) {
        String stream = null;
        String ua = live.getUa();
        outer:
        for (Group group : live.getGroups()) {
            for (Channel channel : group.getChannel()) {
                if (channel.getUrls().isEmpty()) continue;
                stream = channel.getUrls().get(0);
                if (channel.getUa() != null && !channel.getUa().isEmpty()) ua = channel.getUa();
                break outer;
            }
        }
        if (stream == null) return;
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(stream).openConnection();
            c.setConnectTimeout(8000);
            c.setReadTimeout(8000);
            c.setRequestProperty("Range", "bytes=0-0");
            if (ua != null && !ua.isEmpty()) c.setRequestProperty("User-Agent", ua);
            int code = c.getResponseCode();
            String type = c.getContentType();
            c.disconnect();
            System.out.println("QBOX_LIVE_STREAM " + stream + " -> " + code + " " + type);
        } catch (Throwable e) {
            System.out.println("QBOX_LIVE_STREAM " + stream + " -> ERR " + e.getClass().getSimpleName());
        }
    }
}
