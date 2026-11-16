package com.fongmi.android.tv.playback.vod;

import androidx.media3.common.C;

import com.fongmi.android.tv.db.AppDatabase;
import com.fongmi.android.tv.utils.SourceUrl;
import com.github.catvod.utils.Prefers;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 「单集进度」独立存储（与 History 解耦）。
 *
 * <p>FongMi 原生设计里 {@link com.fongmi.android.tv.bean.History} 是<b>按剧一条</b>记录，
 * 用于首页「最近观看」行与历史列表展示（进度条读 History.position/duration）。
 * 但用户点进详情页后，<b>每一集各自有播放进度</b>，这是另一份数据：
 * 58 集看到 5:00，切到 57 集应从头，切回 58 集仍应续播 5:00。
 *
 * <p>因此播放进度的真值放在这里（按 historyKey + 集 URL 索引），History.position
 * 只作为「最后观看那一集」的展示快照同步过去，<b>不再作为起播依据</b>。
 *
 * <p>用 SharedPreferences 存一份 JSON：不碰 Room schema，避免
 * {@code AppDatabase.VERSION} 升级触发 {@code fallbackToDestructiveMigration(true)} 清库；
 * 且 {@code Backup.create()} 会连同所有 Prefers 一起备份、{@code restore()} 自动还原。
 */
public final class EpisodeProgress {

    private static final String KEY = "qbox_episode_progress";
    private static final int MAX_EPISODES_PER_VOD = 200;
    private static final int MAX_VODS = 300;

    private static Store store;

    private EpisodeProgress() {
    }

    /**
     * 集的唯一标识：优先用 URL（换源/重排不影响），无 URL 时退回集名。
     * 解析型 URL（{@code source://episode?id=X&target=...}）target 每次解析都不同，
     * 用 FongMi 上游 {@link com.fongmi.android.tv.utils.SourceUrl#original} 归一化到源集 id，
     * 否则同一集会被当成不同集、进度丢失（GS-81③）。
     */
    public static String episodeKey(String url, String name) {
        String key = url == null || url.trim().isEmpty() ? String.valueOf(name) : url.trim();
        try {
            return SourceUrl.original(key);
        } catch (RuntimeException e) {
            return key; // 非法的 source:// URL 不该让播放链路崩溃
        }
    }

    public static synchronized long position(String historyKey, String episode) {
        Item item = find(historyKey, episode);
        return item == null ? C.TIME_UNSET : item.position;
    }

    public static synchronized long duration(String historyKey, String episode) {
        Item item = find(historyKey, episode);
        return item == null ? C.TIME_UNSET : item.duration;
    }

    public static synchronized void put(String historyKey, String episode, long position, long duration) {
        if (historyKey == null || episode == null || position < 0) return;
        load();
        Map<String, Item> episodes = store.vods.computeIfAbsent(historyKey, k -> new LinkedHashMap<>());
        Item item = episodes.computeIfAbsent(episode, k -> new Item());
        item.position = position;
        item.duration = duration;
        item.time = System.currentTimeMillis();
        trimEpisodes(episodes);
        trimVods();
        persist();
    }

    /** 旧版本升级迁移：仅当该集还没有独立记录时，把 History 里遗留的进度搬过来。 */
    public static synchronized void migrate(String historyKey, String episode, long position, long duration) {
        if (position < 0 || has(historyKey, episode)) return;
        put(historyKey, episode, position, duration);
    }

    public static synchronized void remove(String historyKey, String episode) {
        load();
        Map<String, Item> episodes = store.vods.get(historyKey);
        if (episodes == null || episodes.remove(episode) == null) return;
        if (episodes.isEmpty()) store.vods.remove(historyKey);
        persist();
    }

    public static synchronized void removeVod(String historyKey) {
        load();
        if (store.vods.remove(historyKey) == null) return;
        persist();
    }

    /** 删配置源时按 cid 清理（historyKey 末段即 cid）。 */
    public static synchronized void removeCid(int cid) {
        load();
        String suffix = AppDatabase.SYMBOL + cid;
        boolean changed = store.vods.keySet().removeIf(key -> key != null && key.endsWith(suffix));
        if (changed) persist();
    }

    public static synchronized void clear() {
        load();
        if (store.vods.isEmpty()) return;
        store.vods.clear();
        persist();
    }

    private static boolean has(String historyKey, String episode) {
        return find(historyKey, episode) != null;
    }

    private static Item find(String historyKey, String episode) {
        if (historyKey == null || episode == null) return null;
        load();
        Map<String, Item> episodes = store.vods.get(historyKey);
        return episodes == null ? null : episodes.get(episode);
    }

    private static void trimEpisodes(Map<String, Item> episodes) {
        if (episodes.size() <= MAX_EPISODES_PER_VOD) return;
        List<Map.Entry<String, Item>> sorted = new ArrayList<>(episodes.entrySet());
        sorted.sort(Comparator.comparingLong(e -> e.getValue().time));
        for (int i = 0; i < sorted.size() - MAX_EPISODES_PER_VOD; i++) episodes.remove(sorted.get(i).getKey());
    }

    private static void trimVods() {
        if (store.vods.size() <= MAX_VODS) return;
        List<Map.Entry<String, Map<String, Item>>> sorted = new ArrayList<>(store.vods.entrySet());
        sorted.sort(Comparator.comparingLong(EpisodeProgress::lastTime));
        for (int i = 0; i < sorted.size() - MAX_VODS; i++) store.vods.remove(sorted.get(i).getKey());
    }

    private static long lastTime(Map.Entry<String, Map<String, Item>> entry) {
        long last = 0;
        for (Item item : entry.getValue().values()) last = Math.max(last, item.time);
        return last;
    }

    private static void load() {
        if (store != null) return;
        try {
            String json = Prefers.getString(KEY);
            store = json.isEmpty() ? new Store() : parse(json);
        } catch (Exception e) {
            store = new Store();
        }
        if (store == null || store.vods == null) store = new Store();
    }

    /** 手工解析，避免 R8 抹掉 Gson 泛型签名后把内层 Item 反序列化成 LinkedTreeMap。 */
    private static Store parse(String json) {
        Store store = new Store();
        JsonElement rootElement = JsonParser.parseString(json);
        if (!rootElement.isJsonObject()) return store;
        JsonObject root = rootElement.getAsJsonObject();
        JsonObject vods = object(root, "vods", "a");
        if (vods == null) return store;
        for (Map.Entry<String, JsonElement> vod : vods.entrySet()) {
            if (!vod.getValue().isJsonObject()) continue;
            Map<String, Item> episodes = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : vod.getValue().getAsJsonObject().entrySet()) {
                if (!entry.getValue().isJsonObject()) continue;
                JsonObject object = entry.getValue().getAsJsonObject();
                Item item = new Item();
                item.position = readLong(object, "position", "a", C.TIME_UNSET);
                item.duration = readLong(object, "duration", "b", C.TIME_UNSET);
                item.time = readLong(object, "time", "c", 0);
                episodes.put(entry.getKey(), item);
            }
            store.vods.put(vod.getKey(), episodes);
        }
        return store;
    }

    private static JsonObject object(JsonObject parent, String name, String legacy) {
        JsonElement element = parent.get(name);
        if (element == null || !element.isJsonObject()) element = parent.get(legacy);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static long readLong(JsonObject object, String name, String legacy, long fallback) {
        JsonElement element = object.get(name);
        if (element == null || element.isJsonNull()) element = object.get(legacy);
        if (element == null || element.isJsonNull()) return fallback;
        try {
            return element.getAsLong();
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private static void persist() {
        try {
            JsonObject root = new JsonObject();
            JsonObject vods = new JsonObject();
            for (Map.Entry<String, Map<String, Item>> vod : store.vods.entrySet()) {
                if (vod.getKey() == null || vod.getValue() == null) continue;
                JsonObject episodes = new JsonObject();
                for (Map.Entry<String, Item> entry : vod.getValue().entrySet()) {
                    if (entry.getKey() == null || entry.getValue() == null) continue;
                    Item item = entry.getValue();
                    JsonObject object = new JsonObject();
                    object.addProperty("position", item.position);
                    object.addProperty("duration", item.duration);
                    object.addProperty("time", item.time);
                    episodes.add(entry.getKey(), object);
                }
                vods.add(vod.getKey(), episodes);
            }
            root.add("vods", vods);
            Prefers.put(KEY, root.toString());
        } catch (Exception ignored) {
            // 存储不可用时退化为进程内记忆，不让播放链路崩溃。
        }
    }

    private static final class Store {
        private final Map<String, Map<String, Item>> vods = new LinkedHashMap<>();
    }

    private static final class Item {
        private long position;
        private long duration;
        private long time;
    }
}
