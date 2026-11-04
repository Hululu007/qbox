package com.qbox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.fongmi.android.tv.utils.Util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * N-25 回归：详情页简介把源的 "&nbsp;" 原样显示出来。
 * Util.clean 对不含 "<" 的文本直接返回，漏掉了 HTML 实体清洗。
 * 修复后：无标签文本也要剥掉 &nbsp; / &#160; /   / 　，且不能误伤正常中英文标点与 emoji。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class UtilCleanTest {

    @Test
    public void nbspStrippedWithoutTags() {
        String out = Util.clean("&nbsp;《伦敦合伙人》是由湖南卫视、芒果TV联合出品的青春合伙人开店创业真人秀。");
        assertFalse("still contains &nbsp;: " + out, out.contains("&nbsp;"));
        assertTrue("should keep content, got: " + out, out.contains("《伦敦合伙人》是由湖南卫视"));
        assertTrue("leading nbsp should become trimmed space, got: [" + out + "]", out.startsWith("《伦敦合伙人》"));
    }

    @Test
    public void nbspVariantsStripped() {
        assertFalse(Util.clean("&#160;abc").contains("&#160;"));
        assertFalse(Util.clean(" abc").contains(" "));
        assertFalse(Util.clean("　abc").contains("　"));
    }

    @Test
    public void normalTextUntouched() {
        // 中英文标点、连字符、数字、emoji 不得被改动
        String in = "速度与激情9：2021, F9 — 范·迪塞尔!（中文字幕）";
        assertEquals(in, Util.clean(in));
        String emoji = "测试🎬🎉内容";
        assertEquals(emoji, Util.clean(emoji));
    }

    @Test
    public void htmlTagsStillCleaned() {
        String out = Util.clean("<p>第一段</p><p>第二段&nbsp;尾部</p>");
        assertFalse(out.contains("<p>"));
        assertFalse(out.contains("&nbsp;"));
        assertTrue(out.contains("第一段"));
        assertTrue(out.contains("第二段"));
    }

    @Test
    public void emptyAndBlank() {
        assertEquals("", Util.clean("").trim());
        assertEquals("", Util.clean("&nbsp;").trim());
    }
}
