package com.qbox;

import static org.junit.Assert.assertTrue;

import com.fongmi.android.tv.api.Decoder;
import com.github.catvod.utils.Json;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * QBox 端到端配置链路测试（JVM + Robolectric）：
 * 直接用壳里真正的 Decoder 去拉默认配置，验证：
 * 1) 302 跳转 + JPEG 伪装 + "**" base64 能被正确解出 JSON；
 * 2) sites / spider 字段存在且数量合理；
 * 3) spider 地址带 ;md5; 校验。
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
}
