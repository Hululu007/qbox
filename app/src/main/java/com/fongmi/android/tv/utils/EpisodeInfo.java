package com.fongmi.android.tv.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 选集资源名解析：把「[1.55GB] 剧名 2026.10.31-第4期超前彩 H265 AAC 5.1 1080p」这类文件名
 * 拆成可展示信息——匹配部分（日期/集数/版本修饰）+ 剩余原名（rest，过长聚焦滚动）+ 尾部编码与声道 + 右侧竖排清晰度/影调角标；
 * 同时按「集数多少 + 标签长短」算出「一页几列几行」的分页方案（固定两行；集数少时稀疏宽格）。
 */
public class EpisodeInfo {

    public static final int TIER_SHORT = 0;
    public static final int TIER_MEDIUM = 1;
    public static final int TIER_LONG = 2;

    private static final Pattern SQUARE_CHARS = Pattern.compile("[\\[\\]]");
    /** 压制组/网盘命名里的方括号集数：[DBD-Raws][剧名][01][2160P]。 */
    private static final Pattern BRACKET_EP = Pattern.compile("\\[(\\d{1,4})(?:[vV]\\d)?\\]");
    private static final Pattern SIZE = Pattern.compile("(?i)\\b\\d+(?:\\.\\d+)?\\s*(?:GB|MB|TB)\\b");
    private static final Pattern EXT = Pattern.compile("(?i)\\.(mp4|mkv|avi|ts|flv|mov|wmv|rmvb|webm|m3u8|m2ts|mp3|m4a|flac)$");
    private static final Pattern BRACKET_CHARS = Pattern.compile("[(){}<>【】《》「」『』]");

    private static final Pattern RES_4K = Pattern.compile("(?i)(2160[pi]?|4k|uhd)");
    private static final Pattern RES_FHD = Pattern.compile("(?i)(1080[pi]?|fhd)");
    private static final Pattern RES_HD = Pattern.compile("(?i)(720[pi]?|(?<![a-z0-9])hd(?![a-z0-9]))");
    private static final Pattern RES_SD = Pattern.compile("(?i)(480[pi]?|(?<![a-z0-9])sd(?![a-z0-9]))");
    private static final Pattern DV = Pattern.compile("(?i)(dolby\\s?vision|dovi|(?<![a-z0-9])dv(?![a-z0-9])|杜比视界)");
    private static final Pattern ATMOS = Pattern.compile("(?i)(dolby\\s?atmos|atmos|杜比全景)");
    private static final Pattern HDR = Pattern.compile("(?i)(hdr10?|(?<![a-z0-9])hdr(?![a-z0-9])|hlg)");
    private static final Pattern CODEC_265 = Pattern.compile("(?i)(h\\.?265|hevc|x\\.?265)");
    private static final Pattern CODEC_264 = Pattern.compile("(?i)(h\\.?264|avc|x\\.?264)");
    private static final Pattern CODEC_AV1 = Pattern.compile("(?i)(av1)");
    private static final Pattern AUDIO_CODEC = Pattern.compile("(?i)(aac|ac-?3|e-?ac-?3|dts-?hd|dts|truehd|flac|opus)");
    private static final Pattern CH_71 = Pattern.compile("(?i)((?<![0-9])(?<![0-9]{4}[.\\-/])7\\.1(?:\\.\\d)?(?![0-9]))");
    private static final Pattern CH_51 = Pattern.compile("(?i)((?<![0-9])(?<![0-9]{4}[.\\-/])5\\.1(?:\\.\\d)?(?![0-9]))");
    private static final Pattern CH_20 = Pattern.compile("(?i)((?<![0-9])(?<![0-9]{4}[.\\-/])2\\.0(?![0-9]))|stereo|立体声");
    private static final Pattern CH_MULTI = Pattern.compile("(?i)(多声道|multichannel|multi-?ch(?:annel)?)");
    /** tail 里表示「声道」的段：5.1 / 7.1.3 / 2.0 / 多声道。用于把声道拆到右下角固定显示。 */
    private static final Pattern CHANNEL_TOKEN = Pattern.compile("^\\d+\\.\\d+(?:\\.\\d+)?$|^多声道$");

    private static final Pattern SEASON_EP = Pattern.compile("(?i)(?<![A-Za-z])S(\\d{1,2})[\\s._-]*E(\\d{1,4})(?![0-9])");
    private static final Pattern CN_SEASON_EP = Pattern.compile("第\\s*([0-9一二三四五六七八九十两]+)\\s*季\\s*第\\s*([0-9一二三四五六七八九十两]+)\\s*([集期话回])");
    private static final Pattern CN_EP = Pattern.compile("第\\s*([0-9一二三四五六七八九十百零〇两]+)\\s*([集期话回])(?:\\s*[（(]\\s*([上下中])\\s*[)）]|\\s*([上下中])(?![\\u4e00-\\u9fa5A-Za-z0-9]))?");
    private static final Pattern EP = Pattern.compile("(?i)(?<![A-Za-z])E(?:P)?\\s*(\\d{1,4})(?![0-9])");
    /** 网盘/压制组常见：1x05、Season 1 Episode 5、05集/05话（没有「第」）。 */
    private static final Pattern X_FORM = Pattern.compile("(?i)(?<![0-9A-Za-z])(\\d{1,2})x(\\d{1,4})(?![0-9])");
    private static final Pattern SEASON_WORD = Pattern.compile("(?i)season\\s*(\\d{1,2})\\s*episode\\s*(\\d{1,4})");
    /** 网盘/压制组常见：1x05、Season 1 Episode 5、05集/05话/05期/05回（没有「第」，综艺「07-週05期（上）」靠这条）。 */
    private static final Pattern BARE_EP = Pattern.compile("(?<![0-9A-Za-z第])(\\d{1,4})\\s*([集话期回])(?:\\s*[（(]\\s*([上下中])\\s*[)）]|\\s*([上下中])(?![\\u4e00-\\u9fa5A-Za-z0-9]))?");
    /** 集数前的版本修饰（沉浸版 / 加更版 / 抢鲜版 / 未删减版）：同一天多版本必须能区分。 */
    private static final Pattern QUALIFIER = Pattern.compile("([\\u4e00-\\u9fa5]{1,4}版)$");
    /** 网盘重复下载后缀：(1) (2) —— 不是集数，必须先剔掉，否则 `15(1).mp4` 会被当成第 1 集。 */
    private static final Pattern DUPLICATE = Pattern.compile("[(（]\\s*\\d{1,2}\\s*[)）]");
    private static final Pattern TRAILING_NUMBER = Pattern.compile("(?:^|[\\s\\-_.])(\\d{1,3})[\\s\\-_.]*$");

    private static final Pattern DATE_FULL = Pattern.compile("(?<![0-9A-Za-z])(20\\d{2})[.\\-/年](1[0-2]|0?[1-9])[.\\-/月](3[01]|[12]\\d|0?[1-9])日?(?![0-9A-Za-z])");
    private static final Pattern DATE_COMPACT = Pattern.compile("(?<![0-9A-Za-z])(20\\d{2})(1[0-2]|0?[1-9])(3[01]|[12]\\d|0?[1-9])(?![0-9A-Za-z])");
    private static final Pattern DATE_MD = Pattern.compile("(?<![0-9A-Za-z])(1[0-2]|0?[1-9])[.\\-/月](3[01]|[12]\\d|0?[1-9])日?(?![0-9A-Za-z])");
    private static final Pattern TIME_HM = Pattern.compile("(?<![0-9A-Za-z])([01]?\\d|2[0-3])[:：]([0-5]\\d)(?![0-9])");

    public static class Item {

        public final String raw;
        public final String label;
        /** 匹配（日期/集数）之外的剩余原名：展示时排在匹配部分后面，过长滚动展示（用户 2026-11-13 拍板）。 */
        public final String rest;
        public final String tail;
        public final String badge;
        public final long time;
        /** 识别出的季/集编号（无季 = 1）；识别不出为 0。 */
        public final int season;
        public final int episode;

        Item(String raw, String label, String tail, String badge, long time) {
            this(raw, label, "", tail, badge, time, 0, 0);
        }

        Item(String raw, String label, String rest, String tail, String badge, long time, int season, int episode) {
            this.raw = raw;
            this.label = label;
            this.rest = rest;
            this.tail = tail;
            this.badge = badge;
            this.time = time;
            this.season = season;
            this.episode = episode;
        }

        public boolean hasTime() {
            return time > 0;
        }

        public boolean hasEpisode() {
            return episode > 0;
        }

        /** 右上角标第一排：分辨率（badge 第一行，4K/FHD/HD/SD）。纯派生，不改变 badge 字段。 */
        public String resolution() {
            int nl = badge.indexOf('\n');
            return nl < 0 ? badge : badge.substring(0, nl);
        }

        /** 右上角标第二排：动态范围/画质（badge 第二行去掉 ATMOS；ATMOS 归右下声道区）。 */
        public String dynamicRange() {
            int nl = badge.indexOf('\n');
            if (nl < 0) return "";
            StringBuilder sb = new StringBuilder();
            for (String part : badge.substring(nl + 1).split("·")) {
                if (part.isEmpty() || "ATMOS".equals(part)) continue;
                if (sb.length() > 0) sb.append("·");
                sb.append(part);
            }
            return sb.toString();
        }

        /** 右下角固定声道：tail 里的数字声道/多声道 + badge 里的 ATMOS。 */
        public String channel() {
            String c = "";
            for (String part : tail.split("·")) if (CHANNEL_TOKEN.matcher(part).matches()) c = part;
            boolean atmos = badge.contains("ATMOS");
            return join(c, atmos ? "ATMOS" : "");
        }

        /** 滚动名称尾部：tail 去掉声道段（保留 H265/AAC 等编码），避免与右下声道重复。 */
        public String codecTail() {
            StringBuilder sb = new StringBuilder();
            for (String part : tail.split("·")) {
                if (part.isEmpty() || CHANNEL_TOKEN.matcher(part).matches()) continue;
                if (sb.length() > 0) sb.append("·");
                sb.append(part);
            }
            return sb.toString();
        }
    }

    /** 排版方案：固定两行；短名 7 列（14/页），其余 5 列（10/页）。 */
    public static class Plan {

        public final List<Item> items;
        public final int columns;
        public final int rows;
        public final int groupSize;

        Plan(List<Item> items, int columns, int rows, int groupSize) {
            this.items = items;
            this.columns = columns;
            this.rows = rows;
            this.groupSize = groupSize;
        }

        public int groupCount() {
            return items.isEmpty() ? 0 : (items.size() + groupSize - 1) / groupSize;
        }
    }

    /**
     * 排版方案：固定两行；页内列数按「集数多少 + 标签长短」自适应（用户 2026-11-13 拍板）：
     * 集数多时多列（短名 7 列 / 其余 5 列），集数少（电影/单片）时稀疏排列——
     * 少列宽格能显示更完整的名称信息。第一排满了才换第二排；不足一排时左对齐不拉伸。
     */
    public static Plan analyze(List<String> names) {
        List<Item> items = new ArrayList<>();
        for (String name : names) items.add(parse(name));
        dedupe(items);
        int tier = tier(items);
        int count = items.size();
        int columns;
        if (count <= 4) columns = 2;
        else if (tier == TIER_SHORT) columns = Math.min(count, 7);
        else columns = Math.min(count, 5);
        // 第一排（columns 个）满了才换第二排；内容不多时只有一排，格子不拉宽、从左往右按顺序排。
        int rows = count <= columns ? 1 : 2;
        int groupSize = Math.max(1, columns * rows);
        return new Plan(items, columns, rows, groupSize);
    }

    /** 解析后仍重名（同一天多个版本/同集多条线路）时补序号，保证每一格都能区分。 */
    private static void dedupe(List<Item> items) {
        java.util.Map<String, Integer> seen = new java.util.HashMap<>();
        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            Integer count = seen.get(item.label);
            if (count == null) {
                seen.put(item.label, 1);
                continue;
            }
            int next = count + 1;
            seen.put(item.label, next);
            items.set(i, new Item(item.raw, item.label + " ·" + next, item.rest, item.tail, item.badge, item.time, item.season, item.episode));
        }
    }

    /**
     * 排列顺序（用户 2026-11-13 拍板）：金标准是「尽量让用户能从 1 看到 100」。
     * <ul>
     *   <li>时间都能识别：按时间由早到晚（倒序则由晚到早）。</li>
     *   <li>集数能识别：按集数字大小排（修「E11 排最前、E01 排后面」；季在前）。</li>
     *   <li>两条都有：时间优先（同时间再按集数区分同日多版本）。</li>
     *   <li>混合命名（有的有日期有的只有集数）：有集数的双方先按集数比，
     *       只有双方集数信息不全时才让有时间的一方在前。</li>
     *   <li>都识别不出：保持源顺序。</li>
     * </ul>
     *
     * @return 新顺序对应的原下标
     */
    public static int[] order(List<String> names, boolean desc) {
        int size = names.size();
        int[] order = new int[size];
        for (int i = 0; i < size; i++) order[i] = i;
        if (size < 2) return order;
        long[] times = new long[size];
        int[] episodes = new int[size];
        int[] seasons = new int[size];
        boolean anyTime = false;
        boolean anyEpisode = false;
        for (int i = 0; i < size; i++) {
            Item item = parse(names.get(i));
            times[i] = item.time;
            episodes[i] = item.episode;
            seasons[i] = item.season;
            if (item.time > 0) anyTime = true;
            if (item.episode > 0) anyEpisode = true;
        }
        if (!anyTime && !anyEpisode) {
            if (desc) for (int i = 0; i < size; i++) order[i] = size - 1 - i;
            return order;
        }
        Integer[] index = new Integer[size];
        for (int i = 0; i < size; i++) index[i] = i;
        Arrays.sort(index, (a, b) -> {
            // 1) 双方都有时间：时间由早到晚（同时间由规则 2 接手）
            if (times[a] > 0 && times[b] > 0 && times[a] != times[b]) {
                int compare = Long.compare(times[a], times[b]);
                return desc ? -compare : compare;
            }
            // 2) 双方都有集数：按季/集数字大小——混合命名（时间+集数夹杂）也先保证 1→100 顺序
            if (episodes[a] > 0 && episodes[b] > 0 && (episodes[a] != episodes[b] || seasons[a] != seasons[b])) {
                int compare = seasons[a] != seasons[b] ? Integer.compare(seasons[a], seasons[b]) : Integer.compare(episodes[a], episodes[b]);
                return desc ? -compare : compare;
            }
            // 3) 信息不对等：有时间/有集数的一方在前
            if ((times[a] > 0 || episodes[a] > 0) != (times[b] > 0 || episodes[b] > 0)) return times[a] > 0 || episodes[a] > 0 ? -1 : 1;
            // 4) 都识别不出：保持源顺序
            return desc ? Integer.compare(b, a) : Integer.compare(a, b);
        });
        for (int i = 0; i < size; i++) order[i] = index[i];
        return order;
    }

    /**
     * 选集「边缘翻页」在下一组的落点（GS-78⑥ / GS-79⑦）：forward 到下一组时，
     * 若下一组有「同行」（row*columns < size）落同行首格；否则（不满页，无该行）退化到组开头（0 =
     * 相邻的下一集），而不是 Math.min(target, size-1) 那样跳到末集。
     */
    public static int forwardLanding(int row, int columns, int size) {
        int target = row * columns;
        return target < size ? target : 0;
    }

    /**
     * 选集「边缘翻页」在上一组的落点：backward 到上一组时落「同行末格」（row*columns + columns - 1）；
     * 若上一组没有该行（不满页）则退化到组末（size-1，往回退的相邻就是上一组最后一集）。
     */
    public static int backwardLanding(int row, int columns, int size) {
        return Math.min(row * columns + columns - 1, size - 1);
    }

    public static Item parse(String name) {
        String raw = name == null ? "" : name.trim();
        String text = DUPLICATE.matcher(EXT.matcher(SIZE.matcher(SQUARE_CHARS.matcher(raw).replaceAll(" ")).replaceAll(" ")).replaceAll("")).replaceAll(" ");
        text = BRACKET_CHARS.matcher(text).replaceAll(" ").replaceAll("\\s+", " ").trim();
        Matcher bracket = BRACKET_EP.matcher(raw);
        String bracketEp = bracket.find() ? bracket.group(1) : "";

        String resolution = "";
        Matcher m;
        if (RES_4K.matcher(text).find()) resolution = "4K";
        else if (RES_FHD.matcher(text).find()) resolution = "FHD";
        else if (RES_HD.matcher(text).find()) resolution = "HD";
        else if (RES_SD.matcher(text).find()) resolution = "SD";
        boolean dv = DV.matcher(text).find();
        boolean atmos = ATMOS.matcher(text).find();
        boolean hdr = HDR.matcher(text).find();
        String sound = join(dv ? "DV" : "", atmos ? "ATMOS" : "", hdr ? "HDR" : "");
        String badge = resolution;
        if (!sound.isEmpty()) badge += (badge.isEmpty() ? "" : "\n") + sound;

        String codec = "";
        if (CODEC_265.matcher(text).find()) codec = "H265";
        else if (CODEC_264.matcher(text).find()) codec = "H264";
        else if (CODEC_AV1.matcher(text).find()) codec = "AV1";
        String audio = "";
        Matcher am = AUDIO_CODEC.matcher(text);
        if (am.find()) audio = am.group(1).toUpperCase(Locale.US);
        String channel = "";
        Matcher cm;
        if ((cm = CH_71.matcher(text)).find()) channel = cm.group(1);
        else if ((cm = CH_51.matcher(text)).find()) channel = cm.group(1);
        else if (CH_20.matcher(text).find()) channel = "2.0";
        else if (CH_MULTI.matcher(text).find()) channel = "多声道";
        String tail = join(codec, audio, channel);

        // 先摘掉编码/声道/清晰度，避免 "5.1"、"2.0" 被误判成日期
        String keyText = strip(text, RES_4K, RES_FHD, RES_HD, RES_SD, DV, ATMOS, HDR, CODEC_265, CODEC_264, CODEC_AV1, AUDIO_CODEC, CH_71, CH_51, CH_20, CH_MULTI);

        int year = 0, month = 0, day = 0, hour = 0, minute = 0;
        boolean hasDate = false;
        if ((m = DATE_FULL.matcher(keyText)).find()) {
            year = num(m.group(1));
            month = num(m.group(2));
            day = num(m.group(3));
            hasDate = true;
        } else if ((m = DATE_COMPACT.matcher(keyText)).find()) {
            year = num(m.group(1));
            month = num(m.group(2));
            day = num(m.group(3));
            hasDate = true;
        } else if ((m = DATE_MD.matcher(keyText)).find()) {
            month = num(m.group(1));
            day = num(m.group(2));
            year = Calendar.getInstance().get(Calendar.YEAR);
            hasDate = true;
        }
        Matcher tm = TIME_HM.matcher(keyText);
        if (tm.find()) {
            hour = num(tm.group(1));
            minute = num(tm.group(2));
        }
        long time = 0;
        String dateText = "";
        if (hasDate) {
            time = key(year, month, day, hour, minute);
            dateText = String.format(Locale.US, "%02d.%02d", month, day);
        } else if (hour > 0 || minute > 0) {
            time = key(0, 0, 0, hour, minute);
            dateText = String.format(Locale.US, "%02d:%02d", hour, minute);
        }

        String marker = "";
        int markerStart = -1;
        int markerEnd = -1;
        boolean chineseMarker = false;
        Matcher sm = SEASON_EP.matcher(keyText);
        Matcher csm = CN_SEASON_EP.matcher(keyText);
        Matcher cnm = CN_EP.matcher(keyText);
        Matcher em = EP.matcher(keyText);
        int season = 0;
        int episode = 0;
        if (sm.find()) {
            season = num(sm.group(1));
            episode = num(sm.group(2));
            marker = String.format(Locale.US, "S%02dE%02d", season, episode);
            markerStart = sm.start();
            markerEnd = sm.end();
        } else if (csm.find()) {
            season = cn(csm.group(1));
            episode = cn(csm.group(2));
            marker = "第" + season + "季第" + episode + csm.group(3);
            markerStart = csm.start();
            markerEnd = csm.end();
            chineseMarker = true;
        } else if (cnm.find()) {
            episode = cn(cnm.group(1));
            marker = "第" + episode + cnm.group(2) + part(cnm.group(3), cnm.group(4));
            markerStart = cnm.start();
            markerEnd = cnm.end();
            chineseMarker = true;
        } else if (em.find()) {
            episode = num(em.group(1));
            marker = String.format(Locale.US, "E%02d", episode);
            markerStart = em.start();
            markerEnd = em.end();
        } else if ((sm = X_FORM.matcher(keyText)).find()) {
            season = num(sm.group(1));
            episode = num(sm.group(2));
            marker = String.format(Locale.US, "S%02dE%02d", season, episode);
            markerStart = sm.start();
            markerEnd = sm.end();
        } else if ((sm = SEASON_WORD.matcher(keyText)).find()) {
            season = num(sm.group(1));
            episode = num(sm.group(2));
            marker = String.format(Locale.US, "S%02dE%02d", season, episode);
            markerStart = sm.start();
            markerEnd = sm.end();
        } else if ((cnm = BARE_EP.matcher(keyText)).find()) {
            episode = cn(cnm.group(1));
            marker = "第" + episode + cnm.group(2) + part(cnm.group(3), cnm.group(4));
            markerStart = cnm.start();
            markerEnd = cnm.end();
            chineseMarker = true;
        } else if (!bracketEp.isEmpty()) {
            episode = cn(bracketEp);
            marker = "第" + episode + "集";
            chineseMarker = true;
        }

        // 版本修饰词：同一天会有「加更版 / 抢鲜版 / 沉浸版」多条，全部显示成「第2期」就无法区分。
        String qualifier = "";
        if (!marker.isEmpty() && markerStart > 0) {
            Matcher qm = QUALIFIER.matcher(keyText.substring(0, markerStart));
            if (qm.find()) qualifier = qm.group(1);
        }

        String clean = strip(keyText, DATE_FULL, DATE_COMPACT, DATE_MD, TIME_HM, SEASON_EP, CN_SEASON_EP, CN_EP, EP, X_FORM, SEASON_WORD, BARE_EP);
        clean = clean.replaceAll("[\\-_.,;:、，。；：|/\\\\~]+", " ").replaceAll("\\s+", " ").trim();

        // 集数后面的短后缀（加更/超前彩/上传版…）：只收 1-4 个汉字，不吞分隔符和括号。
        String suffix = "";
        if (chineseMarker && markerEnd >= 0 && markerEnd < keyText.length()) {
            String after = keyText.substring(markerEnd).replaceAll("^[\\s\\-_.,;:、，。；：|/\\\\~（）()]+", "");
            int cut = 0;
            while (cut < after.length() && cut < 4 && isHan(after.charAt(cut))) cut++;
            if (cut > 0) suffix = after.substring(0, cut);
        }

        // 匹配之外的剩余原名：版本修饰词与集数后缀要从剩余部分剔掉（已单独拼进 label）。
        String rest = clean;
        if (!qualifier.isEmpty()) rest = rest.replaceFirst(Pattern.quote(qualifier), " ");
        if (!suffix.isEmpty()) rest = rest.replaceFirst(Pattern.quote(suffix), " ");

        String label;
        boolean trailingNumber = false;
        if (!dateText.isEmpty()) {
            label = dateText;
            if (!marker.isEmpty()) label += " " + qualifier + marker + suffix;
            // 剩余原名不再塞进 label：放到 rest，展示时排在匹配部分后面、过长滚动。
        } else if (!marker.isEmpty()) {
            label = qualifier + marker + suffix;
        } else {
            Matcher tn = TRAILING_NUMBER.matcher(keyText);
            if (tn.find()) {
                label = tn.group(1);
                episode = num(tn.group(1));
                trailingNumber = true;
            } else {
                // 识别不出集数/日期：整段清理后的原名就是 label；过长由 UI 滚动展示，不再截断。
                label = clean;
            }
        }
        if (trailingNumber) {
            // 裸数字已经提升为 label，剩余部分里再留着它会重复展示。
            rest = rest.replaceFirst(Pattern.quote(label) + "\\s*$", " ");
        }
        if (label.isEmpty()) label = raw;
        rest = rest.replaceAll("^[\\s\\-_.,;:、，。；：|/\\\\~]+|[\\s\\-_.,;:、，。；：|/\\\\~]+$", "").replaceAll("\\s+", " ").trim();
        if (!rest.isEmpty() && rest.equals(label)) rest = "";
        return new Item(raw, label, rest, tail, badge, time, season, episode);
    }

    /** 只取时间戳（排序用），比完整解析轻。 */
    public static long parseTime(String name) {
        String raw = name == null ? "" : name.trim();
        String text = DUPLICATE.matcher(EXT.matcher(SIZE.matcher(SQUARE_CHARS.matcher(raw).replaceAll(" ")).replaceAll(" ")).replaceAll("")).replaceAll(" ");
        text = BRACKET_CHARS.matcher(text).replaceAll(" ").replaceAll("\\s+", " ").trim();
        text = strip(text, RES_4K, RES_FHD, RES_HD, RES_SD, DV, ATMOS, HDR, CODEC_265, CODEC_264, CODEC_AV1, AUDIO_CODEC, CH_71, CH_51, CH_20, CH_MULTI);
        Matcher m;
        int year = 0, month = 0, day = 0, hour = 0, minute = 0;
        if ((m = DATE_FULL.matcher(text)).find()) {
            year = num(m.group(1));
            month = num(m.group(2));
            day = num(m.group(3));
        } else if ((m = DATE_COMPACT.matcher(text)).find()) {
            year = num(m.group(1));
            month = num(m.group(2));
            day = num(m.group(3));
        } else if ((m = DATE_MD.matcher(text)).find()) {
            month = num(m.group(1));
            day = num(m.group(2));
            year = Calendar.getInstance().get(Calendar.YEAR);
        }
        Matcher tm = TIME_HM.matcher(text);
        if (tm.find()) {
            hour = num(tm.group(1));
            minute = num(tm.group(2));
        }
        if (month > 0 && day > 0) return key(year, month, day, hour, minute);
        if (hour > 0 || minute > 0) return key(0, 0, 0, hour, minute);
        return 0;
    }

    private static int tier(List<Item> items) {
        if (items.isEmpty()) return TIER_SHORT;
        long total = 0;
        for (Item item : items) total += item.label.length();
        int avg = (int) (total / items.size());
        if (avg <= 6) return TIER_SHORT;
        if (avg <= 11) return TIER_MEDIUM;
        return TIER_LONG;
    }

    private static String strip(String text, Pattern... patterns) {
        String s = text;
        for (Pattern pattern : patterns) s = pattern.matcher(s).replaceAll(" ");
        return s.replaceAll("\\s+", " ").trim();
    }

    private static String join(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.isEmpty()) continue;
            if (sb.length() > 0) sb.append("·");
            sb.append(part);
        }
        return sb.toString();
    }

    /** 集数后缀的「上/下/中」：兼容「第5期上」和「第5期（上）」两种写法。 */
    private static String part(String bracketed, String bare) {
        if (bracketed != null && !bracketed.isEmpty()) return bracketed;
        if (bare != null && !bare.isEmpty()) return bare;
        return "";
    }

    private static boolean isHan(char c) {
        return c >= '\u4e00' && c <= '\u9fa5';
    }

    private static int num(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    /** 中文数字 1-999（第三期、第十二集、第二十三期）。 */
    private static int cn(String text) {
        if (text == null || text.isEmpty()) return 0;
        if (text.matches("\\d+")) return num(text);
        String s = text.replace("两", "二").replace("〇", "零");
        int total = 0, number = 0;
        for (char c : s.toCharArray()) {
            int value = "零一二三四五六七八九".indexOf(c);
            if (value >= 0) {
                number = value;
            } else if (c == '十') {
                if (number == 0) number = 1;
                total += number * 10;
                number = 0;
            } else if (c == '百') {
                if (number == 0) number = 1;
                total += number * 100;
                number = 0;
            }
        }
        return total + number;
    }

    private static long key(int year, int month, int day, int hour, int minute) {
        return (long) year * 100000000L + month * 1000000L + day * 10000L + hour * 100L + minute;
    }
}
