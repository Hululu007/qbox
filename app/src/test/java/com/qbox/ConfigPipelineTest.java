package com.qbox;

import static org.junit.Assert.assertTrue;

import com.github.catvod.utils.Json;
import com.google.gson.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * QBox 配置链路测试（JVM + Robolectric）：
 * 验证内置兜底快照（assets/qbox_default.json）可解析，且公开仓的兜底不内置真实内容源。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, manifest = Config.NONE)
public class ConfigPipelineTest {

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
        assertTrue("missing sites", object.has("sites"));
        // 公开仓的 debug 兜底只保留可解析的空白骨架，不再内置任何真实内容源。
        assertTrue("fallback should be empty", object.getAsJsonArray("sites").size() == 0);
        System.out.println("QBOX_FALLBACK_OK");
    }
}
