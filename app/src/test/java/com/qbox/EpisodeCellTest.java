package com.qbox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Episode;
import com.fongmi.android.tv.ui.adapter.EpisodeAdapter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Arrays;

/**
 * 选集格子绑定回归：解析出的编码/声道要出现在主标签尾部，分辨率/杜比视界要出现在右上角标；
 * 没有清晰度信息时角标必须隐藏。
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class EpisodeCellTest {

    @Test
    public void badgeAndTailBind() {
        Context context = RuntimeEnvironment.getApplication();
        FrameLayout parent = new FrameLayout(context);
        EpisodeAdapter adapter = new EpisodeAdapter(item -> {
        });
        Episode first = new Episode();
        first.setName("庆余年 2160p 杜比视界 H265 5.1");
        Episode second = new Episode();
        second.setName("第2集");
        adapter.addAll(Arrays.asList(first, second));

        EpisodeAdapter.ViewHolder holder = adapter.onCreateViewHolder(parent, 0);
        adapter.onBindViewHolder(holder, 0);
        TextView prefix = holder.itemView.findViewById(R.id.prefix);
        TextView text = holder.itemView.findViewById(R.id.text);
        TextView badge = holder.itemView.findViewById(R.id.badge);
        TextView channel = holder.itemView.findViewById(R.id.channel);
        // 回归：text 不能复制父状态，否则 setSelected(true) 会被 pill/root 覆盖，marquee 不启动。
        assertFalse("scrolling text must own its selected state", text.isDuplicateParentStateEnabled());
        assertEquals(TextUtils.TruncateAt.MARQUEE, text.getEllipsize());
        // 前缀 = 识别到的时间/集数，固定不滚（不含编码）
        assertTrue("prefix should be the recognized label: " + prefix.getText(), prefix.getText().length() > 0);
        // 滚动名 = 原资源全名 + 编码尾部（保留 H265 供人类判断）
        assertTrue("scrolling name should contain codec: " + text.getText(), text.getText().toString().contains("H265"));
        // 右上两排 = 分辨率 / 动态范围
        assertEquals("4K\nDV", badge.getText().toString());
        assertEquals(View.VISIBLE, badge.getVisibility());
        // 右下固定 = 声道（从 tail 拆出，不再混在滚动名里）
        assertEquals("5.1", channel.getText().toString());
        assertEquals(View.VISIBLE, channel.getVisibility());

        adapter.onBindViewHolder(holder, 1);
        assertEquals(View.GONE, badge.getVisibility());
        assertEquals(View.GONE, channel.getVisibility());
    }

    /** 长资源名没有日期/集数结构时，不能整段塞进固定前缀；必须交给可滚动 text。 */
    @Test
    public void unstructuredLongNameGoesToScrollableText() {
        Context context = RuntimeEnvironment.getApplication();
        FrameLayout parent = new FrameLayout(context);
        EpisodeAdapter adapter = new EpisodeAdapter(item -> {
        });
        Episode episode = new Episode();
        episode.setName("[4.30GB] 逃出布迪秀Buddy2026(1) 【佬友 LY-96】逃出布迪秀.Buddy.2026.1080p.AMZN.WEB-DL-LYZM.mkv");
        adapter.addAll(java.util.Collections.singletonList(episode));

        EpisodeAdapter.ViewHolder holder = adapter.onCreateViewHolder(parent, 0);
        adapter.onBindViewHolder(holder, 0);
        TextView prefix = holder.itemView.findViewById(R.id.prefix);
        TextView text = holder.itemView.findViewById(R.id.text);
        assertEquals("", prefix.getText().toString());
        assertTrue("long unstructured name must be scrollable: " + text.getText(), text.getText().toString().contains("[4.30GB]"));
        assertTrue("long unstructured name must keep extension: " + text.getText(), text.getText().toString().endsWith(".mkv"));
        assertFalse(text.isDuplicateParentStateEnabled());
    }

    /** GS-79：稳定 id = 页内槽位 position，跨页稳定，是「换页保持焦点」的前提。 */
    @Test
    public void stableIdsAreSlotBasedForFocusRetention() {
        EpisodeAdapter adapter = new EpisodeAdapter(item -> {
        });
        assertTrue(adapter.hasStableIds());
        assertEquals(0, adapter.getItemId(0));
        assertEquals(3, adapter.getItemId(3));
        Episode first = new Episode();
        first.setName("E01");
        Episode second = new Episode();
        second.setName("E02");
        adapter.addAll(Arrays.asList(first, second));
        adapter.setPage(0, 2);
        assertEquals(1, adapter.getItemId(1));
    }
}
