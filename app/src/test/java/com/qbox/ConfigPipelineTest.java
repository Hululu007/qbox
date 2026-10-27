package com.qbox;

import static org.junit.Assert.assertTrue;

import com.fongmi.android.tv.api.Decoder;
import com.github.catvod.utils.Json;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * QBox 端到端配置链路测试（JVM + Robolectric）：
 * 直接用壳里真正的 Decoder 去拉默认配置，验证：
 * 1) 302 跳转 + JPEG 伪装 + "**" base64 能被正确解出 JSON；
 * 2) sites / spider 字段存在且数量合理；
 * 3) spider 地址带 ;md5; 校验；
 * 4) 内置兜底快照（assets/qbox_default.json）本身可解析。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, manifest = Config.NONE)
public class ConfigPipelineTest {

    @Test
    public void laobaiConfigLoads() throws Exception {
        String json = Decoder.getJson("https://tvbox.laobaitv.net", "qbox-test");
        JsonObject object = Json.parse(json).getAsJsonObject();
        assertTrue("missing sites", object.has("sites"));
        JsonArray sites = object.getAsJsonArray("sites");
        assertTrue("site count too small: " + sites.size(), sites.size() >= 10);
        assertTrue("missing spider", object.has("spider"));
        String spider = object.get("spider").getAsString();
        assertTrue("spider lacks md5: " + spider, spider.contains(";md5;"));
        System.out.println("QBOX_TEST_OK sites=" + sites.size() + " spider=" + spider);
    }

    @Test
    public void bundledFallbackConfigParses() throws Exception {
        String json;
        try (InputStream in = RuntimeEnvironment.getApplication().getAssets().open("qbox_default.json")) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) > 0) out.write(buffer, 0, read);
            json = out.toString("UTF-8");
        }
        JsonObject object = Json.parse(json).getAsJsonObject();
        JsonArray sites = object.getAsJsonArray("sites");
        assertTrue("fallback sites too small: " + sites.size(), sites.size() >= 10);
        assertTrue("fallback spider missing md5", object.get("spider").getAsString().contains(";md5;"));
        System.out.println("QBOX_FALLBACK_OK sites=" + sites.size());
    }
}
