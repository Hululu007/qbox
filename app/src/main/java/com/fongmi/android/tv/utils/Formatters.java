package com.fongmi.android.tv.utils;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class Formatters {

    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT);
    public static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);
    public static final DateTimeFormatter TIME_SEC = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ROOT).withZone(ZoneId.systemDefault());
    public static final DateTimeFormatter LOCAL_DATETIME = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss", Locale.ROOT);
    public static final DateTimeFormatter EPG_DT_SHORT = DateTimeFormatter.ofPattern("yyyy-MM-ddHH:mm", Locale.ROOT);
    public static final DateTimeFormatter EPG_DT_LONG = DateTimeFormatter.ofPattern("yyyy-MM-ddHH:mm:ss", Locale.ROOT);
    public static final DateTimeFormatter EPG_RANGE = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'", Locale.ROOT).withZone(ZoneOffset.UTC);
    public static final DateTimeFormatter EPG_FULL = DateTimeFormatter.ofPattern("yyyyMMddHHmmss Z", Locale.ROOT);
    public static final DateTimeFormatter EPG_FULL_NO_TZ = DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.ROOT);
    public static final DateTimeFormatter EPG_FULL_COLON = DateTimeFormatter.ofPattern("yyyyMMddHHmmss XXX", Locale.ROOT);

    /** 毫秒 → HH:MM:SS（DLNA 时间；<=0 返回 00:00:00）。 */
    public static String formatMs(long ms) {
        if (ms <= 0) return "00:00:00";
        long s = ms / 1000;
        return String.format(Locale.US, "%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60);
    }

}
