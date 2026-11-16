package com.fongmi.android.tv.gson;

import com.fongmi.android.tv.bean.SkipSegment;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 跳过片段反序列化适配器，FongMi 上游 backport（GS-82 批次 2 的配套件，见 log §50）。
 *
 * <p>内容源下发的 {@code skipSegments} 可能是对象/字符串/畸形数组，直接按 {@code List<SkipSegment>}
 * 反序列化会抛 JsonSyntaxException 打挂<b>整份 Result</b>（详情页/播放结果全废）。
 * 这里：① 非数组一律退化为空列表；② 逐条 try/catch；③ 用 {@link SkipSegment#isValid()} 过滤非法片段。
 */
public class SkipAdapter implements JsonDeserializer<List<SkipSegment>> {

    @Override
    public List<SkipSegment> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (json == null || !json.isJsonArray()) return Collections.emptyList();
        List<SkipSegment> segments = new ArrayList<>();
        for (JsonElement item : json.getAsJsonArray()) addSegment(segments, item, context);
        return segments;
    }

    private void addSegment(List<SkipSegment> segments, JsonElement item, JsonDeserializationContext context) {
        if (!item.isJsonObject()) return;
        try {
            SkipSegment segment = context.deserialize(item, SkipSegment.class);
            if (segment != null && segment.isValid()) segments.add(segment);
        } catch (JsonParseException ignored) {
        }
    }
}
