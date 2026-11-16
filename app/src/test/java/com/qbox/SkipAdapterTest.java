package com.qbox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.SkipSegment;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * FongMi 上游 gson/SkipAdapter backport 回归（GS-82 批次 2 配套件，log §50）。
 *
 * <p>核心回归：内容源把 {@code skipSegments} 写成非数组/畸形值时，旧实现会让 Gson 抛
 * JsonSyntaxException，而 {@code Result.objectFrom()} 捕获后返回 {@code Result.empty()} ——
 * 整份播放结果（含 url/flag/list）全丢。SkipAdapter 保证最坏退化为「无跳过片段」，
 * 其它字段完好。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class SkipAdapterTest {

    /** 畸形（对象）skipSegments 不得打挂整份 Result：其它字段必须保留。 */
    @Test
    public void malformedSkipSegmentsKeepsWholeResult() {
        Result result = Result.objectFrom("{\"flag\":\"line\",\"skipSegments\":{\"type\":\"opening\"},\"playUrl\":\"http://x/a.m3u8\"}");
        assertNotNull(result);
        assertEquals("line", result.getFlag());
        assertEquals("http://x/a.m3u8", result.getPlayUrl());
        assertEquals(0, result.getSkipSegments().size());
    }

    /** skipSegments 是字符串也只退化为空列表。 */
    @Test
    public void scalarSkipSegmentsDegradesToEmpty() {
        Result result = Result.objectFrom("{\"flag\":\"line\",\"skipSegments\":\"oops\"}");
        assertEquals("line", result.getFlag());
        assertEquals(0, result.getSkipSegments().size());
    }

    /** 合法的片头/中段片段保留。 */
    @Test
    public void validSegmentsAreKept() {
        Result result = Result.objectFrom("{\"skipSegments\":[{\"type\":\"opening\",\"start\":0,\"end\":42000},{\"type\":\"middle\",\"start\":10000,\"end\":20000}]}");
        assertEquals(2, result.getSkipSegments().size());
        SkipSegment first = result.getSkipSegments().get(0);
        assertEquals(true, first.isOpening());
        assertEquals(42000, first.getEnd());
    }

    /** 非法片段（start<0 / 未知 type / end<=start / 非对象）逐条过滤，不炸整包。 */
    @Test
    public void invalidSegmentsAreFiltered() {
        String json = "{\"flag\":\"line\",\"skipSegments\":["
                + "{\"type\":\"opening\",\"start\":0,\"end\":42000},"
                + "{\"type\":\"opening\",\"start\":-1,\"end\":42000},"
                + "{\"type\":\"bogus\",\"start\":1,\"end\":2},"
                + "{\"type\":\"middle\",\"start\":900,\"end\":100},"
                + "\"not-an-object\","
                + "42]}";
        Result result = Result.objectFrom(json);
        assertEquals("line", result.getFlag());
        assertEquals(1, result.getSkipSegments().size());
        assertEquals(true, result.getSkipSegments().get(0).isOpening());
    }

    /** 数组里某条缺字段导致反序列化异常时被吞掉，其余仍保留。 */
    @Test
    public void missingFieldsDoNotBreakWholeList() {
        Result result = Result.objectFrom("{\"skipSegments\":[{\"type\":\"opening\"},{\"type\":\"ending\",\"start\":60000}]}");
        // opening 缺 end -> isToEnd() 需 ending 才合法，故不合法；ending 无 end -> toEnd 合法
        assertEquals(1, result.getSkipSegments().size());
        assertEquals(true, result.getSkipSegments().get(0).isEnding());
    }

    @Test
    public void absentSkipSegmentsIsEmpty() {
        Result result = Result.objectFrom("{\"flag\":\"line\"}");
        assertEquals(0, result.getSkipSegments().size());
    }
}
