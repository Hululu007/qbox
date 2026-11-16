package com.qbox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.fongmi.android.tv.utils.EpisodeInfo;

import org.junit.Test;

/**
 * 网盘（夸克 / 阿里 / 百度）资源命名专项回归。
 *
 * 网盘里的文件是「分享者/压制组」随意命名的，常见形态：
 * 容量前缀 + 剧名 + 年份 + 季集 + 清晰度 + 编码 + 声道 + 压制组后缀，
 * 或者只有裸数字 / 1x05 / 05集 / E01 / S01E05 / 第N集 / 第N期上。
 * 这里的用例全部来自真实网盘资源命名模板，按来源分组。
 */
public class QuarkNamingTest {

    private static EpisodeInfo.Item parse(String name) {
        return EpisodeInfo.parse(name);
    }

    /** 1. 玩偶哥哥（夸克分享）：容量前缀 + 拼音剧名 + 集号。 */
    @Test
    public void source1_woGG_sizePrefixAndEPrefix() {
        EpisodeInfo.Item item = parse("[5.38GB] JIAJINCHAI8-1 E01.mkv");
        assertEquals("E01", item.label);
        assertFalse(item.hasTime());
    }

    /** 1b. 玩偶哥哥首条：容量 + 剧名 + 00 + 4K（正片之前的预告/花絮）。 */
    @Test
    public void source1_woGG_zeroAnd4K() {
        EpisodeInfo.Item item = parse("[64.57MB] JIAJINCHAI8-1 00 4K .mp4");
        assertEquals("00", item.label);
        assertEquals("4K", item.badge);
    }

    /** 2. 网盘转存：剧名 + 第N集 + 清晰度。 */
    @Test
    public void source2_kuaFu_chineseEpisode() {
        EpisodeInfo.Item item = parse("嫁金钗 第01集 2160p H265 AAC 5.1.mp4");
        assertEquals("第1集", item.label);
        assertEquals("H265·AAC·5.1", item.tail);
        assertEquals("4K", item.badge);
    }

    /** 3. 压制组 WEB-DL：剧名.年份.S01E01.2160p.WEB-DL.H265.DDP5.1-GROUP。 */
    @Test
    public void source3_webdl_seasonEpisode() {
        EpisodeInfo.Item item = parse("嫁金钗.2026.S01E01.2160p.WEB-DL.H265.DDP5.1-OurTV.mkv");
        assertEquals("S01E01", item.label);
        assertEquals("4K", item.badge);
        assertEquals("H265·5.1", item.tail);
    }

    /** 4. 动漫组方括号堆叠：[组名][剧名][01][2160P][BDRip][HEVC-10bit][FLAC]。 */
    @Test
    public void source4_fansub_bracketStack() {
        EpisodeInfo.Item item = parse("[DBD-Raws][嫁金钗][01][2160P][BDRip][HEVC-10bit][FLAC].mkv");
        assertEquals("第1集", item.label);
        assertEquals("4K", item.badge);
        assertTrue("HEVC 应识别成 H265: " + item.tail, item.tail.contains("H265"));
    }

    /** 5. 网盘裸数字：01.mp4 / 1.mp4 / 05.mkv。 */
    @Test
    public void source5_quark_bareNumber() {
        assertEquals("01", parse("01.mp4").label);
        assertEquals("1", parse("1.mkv").label);
        assertEquals("05", parse("05.mkv").label);
    }

    /** 6. 剧名.集号.清晰度（点在中间）：嫁金钗.01.2160p.mkv。 */
    @Test
    public void source6_quark_dotSeparated() {
        EpisodeInfo.Item item = parse("嫁金钗.01.2160p.mkv");
        assertEquals("01", item.label);
        assertEquals("4K", item.badge);
    }

    /** 7. 海外命名 + 杜比：The.Story.S01E05.1080p.BluRay.x265.DV.Atmos.mkv。 */
    @Test
    public void source7_overseas_dolby() {
        EpisodeInfo.Item item = parse("The.Story.S01E05.1080p.BluRay.x265.DV.Atmos.mkv");
        assertEquals("S01E05", item.label);
        assertEquals("FHD\nDV·ATMOS", item.badge);
        assertEquals("H265", item.tail);
    }

    /** 8. 1x05 命名（欧美剧常见）。 */
    @Test
    public void source8_xForm() {
        assertEquals("S01E05", parse("The.Story.1x05.1080p.mkv").label);
        assertEquals("S02E11", parse("Some Show Season 2 Episode 11.mp4").label);
    }

    /** 9. 没有「第」的集数：05集 / 05话。 */
    @Test
    public void source9_bareJi() {
        assertEquals("第5集", parse("嫁金钗 05集.mp4").label);
        assertEquals("第12话", parse("某番剧 12话 1080p.mp4").label);
    }

    /** 10. 综艺：日期 + 第N期上/下 + 加更/超前彩。 */
    @Test
    public void source10_variety_dateAndPart() {
        assertEquals("11.01 第5期上", parse("[3.04GB] Xz出发 2026.11.1-第5期上.mp4").label);
        assertEquals("10.31 第4期超前彩", parse("[1.55GB] Xz出发 2026.10.31-第4期超前彩 H265 5.1 1080p").label);
        assertEquals("第3期下", parse("H【后！会！友！其！月】第3期下 1080p H265 AAC 2.0.mp4").label);
    }

    /** 11. 中文季集：第2季第5集。 */
    @Test
    public void source11_chineseSeason() {
        assertEquals("第2季第5集", parse("嫁金钗 第2季第5集 1080p.mp4").label);
    }

    /** 12. 全季合集/未识别：不能把年份或容量当成集数。 */
    @Test
    public void source12_yearNotEpisode() {
        EpisodeInfo.Item item = parse("嫁金钗 2026 全30集合集 4K.mp4");
        assertFalse("2026 不能当集数", "2026".equals(item.label));
        assertEquals("4K", item.badge);
    }

    /** 13. 多声道标注。 */
    @Test
    public void source13_multichannel() {
        EpisodeInfo.Item item = parse("嫁金钗 第05集 2160p 多声道.mp4");
        assertEquals("第5集", item.label);
        assertTrue("多声道应进尾部: " + item.tail, item.tail.contains("多声道"));
    }

    /** 14. 7.1.3 完整声道不得被 7.1 截断。 */
    @Test
    public void source14_channel713() {
        EpisodeInfo.Item item = parse("嫁金钗 第05集 2160p DTS 7.1.3.mp4");
        assertTrue("7.1.3 必须完整: " + item.tail, item.tail.contains("7.1.3"));
    }

    /** 15. 会友期式命名：日期 + 版本修饰 + 第N期（上/下），同一天多版本不能重名。 */
    @Test
    public void source15_varietyVersionQualifier() {
        EpisodeInfo.Item a = parse("[4.67GB] H【后!会!友!其月】2026.10.22_沉浸版第1期_4K.mp4");
        EpisodeInfo.Item b = parse("[2.23GB] H【后!会!友!其月】2026.10.30_加更版第2期_4K.mp4");
        EpisodeInfo.Item c = parse("[3.13GB] H【后!会!友!其月】2026.10.30_抢鲜版第2期_4K.mp4");
        EpisodeInfo.Item d = parse("[5.58GB] H【后!会!友!其月】2026.10.29_第3期（下）_4K.mp4");
        assertEquals("10.22 沉浸版第1期", a.label);
        assertEquals("10.30 加更版第2期", b.label);
        assertEquals("10.30 抢鲜版第2期", c.label);
        assertEquals("10.29 第3期下", d.label);
        assertEquals("4K", a.badge);
    }

    /** 17. 真实 4K 原盘命名：DTS-X.7.1 必须识别成声道，不能被当成「7 月 1 日」。 */
    @Test
    public void source17_dtsChannelIsNotDate() {
        EpisodeInfo.Item item = parse("[12.37GB] 战狼2Wolf Warrior II 2017..UHD.Blu-ray.2160p.HEVC.HDR.DTS-X.7.1-DDR.mkv");
        assertFalse("DTS-X.7.1 不能被当成 7 月 1 日，label=" + item.label, item.hasTime());
        assertEquals("4K\nHDR", item.badge);
        assertTrue("7.1 要进尾部: " + item.tail, item.tail.contains("7.1"));
        assertTrue("DTS 要进尾部: " + item.tail, item.tail.contains("DTS"));
    }

    /** 18. 真正的日期（2026.7.1）不能被声道规则吃掉。 */
    @Test
    public void source18_realDateWithSingleDigits() {
        EpisodeInfo.Item item = parse("某综艺 2026.7.1 第5期 1080p");
        assertTrue("2026.7.1 应识别成日期", item.hasTime());
        assertEquals("07.01 第5期", item.label);
    }

    /** 19. 网盘重复下载后缀 (1)：不是集数，15(1) 必须还是第 15 集。 */
    @Test
    public void source19_duplicateSuffixIsNotEpisode() {
        EpisodeInfo.Item a = EpisodeInfo.parse("[617.60MB]15(1).mp4");
        assertEquals("15", a.label);
        assertEquals(15, a.episode);
        EpisodeInfo.Item b = EpisodeInfo.parse("[1.51GB]01-4K(1).mkv");
        assertEquals("01", b.label);
        assertEquals(1, b.episode);
        assertEquals("4K", b.badge);
    }

    /** 16. 同一份列表里若有完全相同的标签，必须补序号，不能两格一模一样。 */
    @Test
    public void duplicateLabelsGetIndex() {
        EpisodeInfo.Plan plan = EpisodeInfo.analyze(java.util.Arrays.asList(
                "[1GB] 剧名 2026.10.30 第2期 4K.mp4",
                "[2GB] 剧名 2026.10.30 第2期 4K.mp4",
                "[3GB] 剧名 2026.10.30 第2期 4K.mp4"));
        assertEquals("10.30 第2期", plan.items.get(0).label);
        assertEquals("10.30 第2期 ·2", plan.items.get(1).label);
        assertEquals("10.30 第2期 ·3", plan.items.get(2).label);
    }
}
