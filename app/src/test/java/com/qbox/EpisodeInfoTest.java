package com.qbox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.fongmi.android.tv.utils.EpisodeInfo;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 选集资源名解析回归（多源模板）：
 * 日期/时间前置并按时间排序；S01E05、第N季第N集、E04、第N期上下、纯数字都要识别；
 * H265/AAC/2.0/5.1/7.1.3 放尾部；4K/FHD + DV/ATMOS/HDR 放右侧竖排角标；
 * 固定两行分页：短名 7×2=14，其余 5×2=10。
 */
public class EpisodeInfoTest {

    @Test
    public void parseDateEpisodeTagAndBadge() {
        EpisodeInfo.Item item = EpisodeInfo.parse("[1.55GB] 剧名 2026.10.31-第4期超前彩 H265 5.1 1080p");
        assertEquals("10.31 第4期超前彩", item.label);
        assertEquals("匹配之外的原名进 rest（2026-11-13 拍板：不丢原名）", "剧名", item.rest);
        assertEquals("H265·5.1", item.tail);
        assertEquals("FHD", item.badge);
        assertTrue(item.hasTime());
    }

    @Test
    public void parseJiaJinChaiE01() {
        EpisodeInfo.Item item = EpisodeInfo.parse("[5.38GB] JIAJINCHAI8-1 E01.mkv");
        assertEquals("E01", item.label);
        assertFalse("JIAJINCHAI8-1 的 8-1 不得识别成日期", item.hasTime());
        assertEquals("", item.tail);
        assertEquals("", item.badge);
    }

    @Test
    public void parseTitleCodeWithTrailingNumber() {
        EpisodeInfo.Item item = EpisodeInfo.parse("[64.57MB] JIAJINCHAI8-1 00 4K .mp4");
        assertEquals("00", item.label);
        assertEquals("4K", item.badge);
    }

    @Test
    public void parseDateVarietyWithSuffix() {
        EpisodeInfo.Item item = EpisodeInfo.parse("[3.04GB] Xz出发 2026.11.1-第5期上传版.mkv");
        assertEquals("11.01 第5期上传版", item.label);
        assertTrue(item.hasTime());
    }

    @Test
    public void parseSeasonEpisode() {
        EpisodeInfo.Item item = EpisodeInfo.parse("S01E05 2160p DV Atmos 5.1");
        assertEquals("S01E05", item.label);
        assertEquals("4K\nDV·ATMOS", item.badge);
        assertEquals("5.1", item.tail);
    }

    @Test
    public void parseChineseSeasonEpisode() {
        EpisodeInfo.Item item = EpisodeInfo.parse("第2季第5集 1080p H265");
        assertEquals("第2季第5集", item.label);
        assertEquals("FHD", item.badge);
        assertEquals("H265", item.tail);
    }

    @Test
    public void parseChineseNumberVarietyPart() {
        EpisodeInfo.Item item = EpisodeInfo.parse("第三期上 4K");
        assertEquals("第3期上", item.label);
        assertEquals("4K", item.badge);
    }

    @Test
    public void parseBareNumber() {
        assertEquals("05", EpisodeInfo.parse("05").label);
        assertEquals("12", EpisodeInfo.parse("12.mp4").label);
    }

    @Test
    public void parseDateMarkerWithExtraSuffix() {
        EpisodeInfo.Item item = EpisodeInfo.parse("Movie 2026.10.31 第4期加更 5.1");
        assertEquals("10.31 第4期加更", item.label);
        assertEquals("5.1", item.tail);
    }

    @Test
    public void parseDolbyVisionAtmosAndChannel() {
        EpisodeInfo.Item item = EpisodeInfo.parse("庆余年 Dolby Vision 2160p Dolby Atmos 7.1.3");
        assertEquals("庆余年", item.label);
        assertTrue(item.badge.contains("4K"));
        assertTrue(item.badge.contains("DV"));
        assertTrue(item.badge.contains("ATMOS"));
        assertEquals("7.1.3", item.tail);
    }

    @Test
    public void parseChineseBracketTitle() {
        EpisodeInfo.Item item = EpisodeInfo.parse("H【后！会！友！其！月】第3期下 1080p H265 AAC 2.0");
        assertEquals("第3期下", item.label);
        assertEquals("FHD", item.badge);
        assertEquals("H265·AAC·2.0", item.tail);
    }

    @Test
    public void channelIsNotMistakenForDate() {
        EpisodeInfo.Item item = EpisodeInfo.parse("流浪地球2 5.1 1080p H264");
        assertFalse("5.1 不得当成 5 月 1 日", item.hasTime());
        assertEquals("H264·5.1", item.tail);
        assertEquals("FHD", item.badge);
    }

    @Test
    public void surroundChannelsPreserved() {
        EpisodeInfo.Item item = EpisodeInfo.parse("电影 7.1.3 Atmos 2160p");
        assertTrue("7.1.3 should be kept: " + item.tail, item.tail.contains("7.1.3"));
        assertTrue(item.badge.contains("4K"));
        assertTrue(item.badge.contains("ATMOS"));
    }

    @Test
    public void shortNamesUseSevenByTwo() {
        List<String> names = new ArrayList<>();
        for (int i = 1; i <= 25; i++) names.add(String.format("%02d", i));
        EpisodeInfo.Plan plan = EpisodeInfo.analyze(names);
        assertEquals(7, plan.columns);
        assertEquals(2, plan.rows);
        assertEquals(14, plan.groupSize);
        assertEquals(2, plan.groupCount());
    }

    @Test
    public void mediumNamesUseFiveByTwo() {
        List<String> names = new ArrayList<>();
        for (int i = 1; i <= 30; i++) names.add(String.format("2026.10.%02d 第%d期", i, i));
        EpisodeInfo.Plan plan = EpisodeInfo.analyze(names);
        assertEquals(5, plan.columns);
        assertEquals(2, plan.rows);
        assertEquals(10, plan.groupSize);
        assertEquals(3, plan.groupCount());
    }

    @Test
    public void longNamesUseThreeByTwo() {
        List<String> names = new ArrayList<>();
        for (int i = 1; i <= 30; i++) names.add(String.format("[1.55GB] 这是一部名字特别长的电视剧资源文件第%02d个版本未删减", i));
        EpisodeInfo.Plan plan = EpisodeInfo.analyze(names);
        assertEquals(3, plan.columns);
        assertEquals(2, plan.rows);
        assertEquals(6, plan.groupSize);
        assertEquals(5, plan.groupCount());
    }

    @Test
    public void moderatelyLongNamesUseFourByTwo() {
        List<String> names = new ArrayList<>();
        for (int i = 1; i <= 30; i++) names.add(String.format("2026.10.%02d 第%d期 高清版", i, i));
        EpisodeInfo.Plan plan = EpisodeInfo.analyze(names);
        assertEquals(4, plan.columns);
        assertEquals(2, plan.rows);
        assertEquals(8, plan.groupSize);
        assertEquals(4, plan.groupCount());
    }

    @Test
    public void orderByTimeAscendingAndDescending() {
        List<String> names = Arrays.asList("2026.10.31", "2026.10.13", "2026.11.07");
        int[] asc = EpisodeInfo.order(names, false);
        assertEquals(1, asc[0]);
        assertEquals(0, asc[1]);
        assertEquals(2, asc[2]);
        int[] desc = EpisodeInfo.order(names, true);
        assertEquals(2, desc[0]);
        assertEquals(0, desc[1]);
        assertEquals(1, desc[2]);
    }

    @Test
    public void orderByEpisodeNumberWhenNoTime() {
        List<String> names = Arrays.asList("E11", "E01", "E05");
        int[] asc = EpisodeInfo.order(names, false);
        assertEquals("E01 必须排第一", 1, asc[0]);
        assertEquals("E05 排第二", 2, asc[1]);
        assertEquals("E11 排最后", 0, asc[2]);
        int[] desc = EpisodeInfo.order(names, true);
        assertEquals(0, desc[0]);
        assertEquals(2, desc[1]);
        assertEquals(1, desc[2]);
    }

    @Test
    public void parseBareQiAndHuiWithoutDi() {
        // 综艺网盘命名「07-週05期（上）」：没有「第」的 05期/05回 也要识别（排序金标准：1→100）。
        EpisodeInfo.Item item = EpisodeInfo.parse("07-週05期（上） 4K.mp4");
        assertEquals("第5期上", item.label);
        assertEquals(5, item.episode);
        EpisodeInfo.Item item2 = EpisodeInfo.parse("[4.2GB] 节目 11-週07期.mp4");
        assertEquals(7, item2.episode);
    }

    @Test
    public void orderBareQiVarietySortsByNumber() {
        List<String> names = Arrays.asList("07-週05期（上）", "07-週07期", "07-週06期（下）");
        int[] asc = EpisodeInfo.order(names, false);
        assertEquals("05期 排第一", 0, asc[0]);
        assertEquals("06期 排第二", 2, asc[1]);
        assertEquals("07期 排最后", 1, asc[2]);
    }

    @Test
    public void fewItemsGetSparseWideCells() {
        // 用户 2026-11-13 拍板：电影/单片集数少时排列稀疏，宽格能显示更多名称信息。
        EpisodeInfo.Plan single = EpisodeInfo.analyze(Arrays.asList("正片"));
        assertEquals(2, single.columns);
        EpisodeInfo.Plan movie = EpisodeInfo.analyze(Arrays.asList("国语版", "粤语版", "导演剪辑版", "未删减版"));
        assertEquals(2, movie.columns);
        assertEquals(2, movie.rows);
        EpisodeInfo.Plan week = EpisodeInfo.analyze(Arrays.asList("第1集", "第2集", "第3集", "第4集", "第5集"));
        assertEquals("5 条短名一排摊开", 5, week.columns);
        assertEquals(1, week.rows);
    }

    @Test
    public void longNamesNotTruncatedKeepRestForMarquee() {
        EpisodeInfo.Item item = EpisodeInfo.parse("[64.57MB] 这是一部名字特别长的电视剧资源文件第07个版本未删减.mkv");
        assertTrue("原名不再截断到 12 字，交给 UI 滚动", item.label.length() >= 12);
    }

    @Test
    public void orderMixedTimeAndEpisodeKeepsNumericOrder() {
        // 混合命名（有的带日期、有的只有集数）：集数可双方比较时先保证 1→100 顺序。
        List<String> names = Arrays.asList("第11集", "2026.10.13 第2集", "2026.11.07 第5集", "第1集", "第7集");
        int[] asc = EpisodeInfo.order(names, false);
        assertEquals("E01 在第一", 3, asc[0]);
        assertEquals("E02 第二", 1, asc[1]);
        assertEquals("E05 第三", 2, asc[2]);
        assertEquals("E07 第四", 4, asc[3]);
        assertEquals("E11 最后", 0, asc[4]);
    }

    @Test
    public void orderSameDayVersionsSortByEpisode() {
        // 同一天多条版本：时间相同，靠集数/源顺序区分；倒序反过来。
        List<String> names = Arrays.asList("2026.10.31 第2期", "2026.10.31 第1期", "2026.10.31 第4期", "2026.10.31 第3期");
        int[] asc = EpisodeInfo.order(names, false);
        assertEquals(1, asc[0]);
        assertEquals(0, asc[1]);
        assertEquals(3, asc[2]);
        assertEquals(2, asc[3]);
    }

    @Test
    public void orderWithoutTimeFallsBackToReverse() {
        List<String> names = Arrays.asList("第1集", "第2集", "第3集");
        int[] asc = EpisodeInfo.order(names, false);
        assertEquals(0, asc[0]);
        assertEquals(1, asc[1]);
        assertEquals(2, asc[2]);
        int[] desc = EpisodeInfo.order(names, true);
        assertEquals(2, desc[0]);
        assertEquals(1, desc[1]);
        assertEquals(0, desc[2]);
    }

    /** 显示分区派生（GS-78）：badge 拆右上两排、tail 拆右下声道 + 滚动名编码，字段本身不变。 */
    @Test
    public void deriveDisplayParts() {
        EpisodeInfo.Item a = EpisodeInfo.parse("S01E05 2160p DV Atmos 5.1");
        assertEquals("4K", a.resolution());
        assertEquals("DV", a.dynamicRange());          // ATMOS 归右下声道区，不留在右上
        assertEquals("5.1·ATMOS", a.channel());
        assertEquals("", a.codecTail());

        EpisodeInfo.Item b = EpisodeInfo.parse("[1.55GB] 剧名 2026.10.31-第4期 H265 AAC 2.0 1080p");
        assertEquals("FHD", b.resolution());
        assertEquals("", b.dynamicRange());
        assertEquals("2.0", b.channel());
        assertEquals("H265·AAC", b.codecTail());

        EpisodeInfo.Item c = EpisodeInfo.parse("第2集");
        assertEquals("", c.resolution());
        assertEquals("", c.dynamicRange());
        assertEquals("", c.channel());
        assertEquals("", c.codecTail());
    }

    /** 选集边缘翻页落点（GS-78⑥ / GS-79⑦）：满→不满退化到组开头（相邻），不跳末集。 */
    @Test
    public void flipLanding() {
        // forward：满页14 → 不满4（row=1，7>=4）→ 0（组开头 = 相邻的下一集）
        assertEquals(0, EpisodeInfo.forwardLanding(1, 7, 4));
        // forward：满页 → 满页（row=1）→ 7（同行首格）
        assertEquals(7, EpisodeInfo.forwardLanding(1, 7, 14));
        // forward：row=0 → 0
        assertEquals(0, EpisodeInfo.forwardLanding(0, 7, 14));
        assertEquals(0, EpisodeInfo.forwardLanding(0, 7, 2));
        // backward：满页 row=0 → 6（同行末格）
        assertEquals(6, EpisodeInfo.backwardLanding(0, 7, 14));
        // backward：满页 row=1 → 13（第二行末格）
        assertEquals(13, EpisodeInfo.backwardLanding(1, 7, 14));
        // backward：不满页 size=2 → size-1=1（组末）
        assertEquals(1, EpisodeInfo.backwardLanding(0, 7, 2));
    }
}
