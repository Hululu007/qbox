package com.qbox;

import static org.junit.Assert.assertEquals;

import com.fongmi.android.tv.api.parser.LiveParser;
import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.bean.Group;
import com.fongmi.android.tv.bean.Live;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * QBox 直播解析测试（JVM + Robolectric）：
 * 用壳里真正的 LiveParser 解析本地 M3U 文本，验证分组、频道与顺序。
 * 不依赖任何外部地址，公开仓与 CI 都可重复运行。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, manifest = Config.NONE)
public class LiveParseTest {

    @Test
    public void localM3uParse() {
        Live live = new Live();
        String m3u = "#EXTM3U\n"
                + "#EXTINF:-1 group-title=\"测试分组\",频道 A\nhttp://127.0.0.1/a.m3u8\n"
                + "#EXTINF:-1 group-title=\"测试分组\",频道 B\nhttp://127.0.0.1/b.m3u8\n";
        LiveParser.text(live, m3u);

        assertEquals(1, live.getGroups().size());
        Group group = live.getGroups().get(0);
        assertEquals("测试分组", group.getName());
        assertEquals(2, group.getChannel().size());
        Channel first = group.getChannel().get(0);
        assertEquals("频道 A", first.getName());
        assertEquals("http://127.0.0.1/a.m3u8", first.getUrls().get(0));
    }
}
