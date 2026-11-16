package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.bean.Episode;
import com.fongmi.android.tv.databinding.AdapterEpisodeBinding;
import com.fongmi.android.tv.utils.EpisodeInfo;
import com.fongmi.android.tv.utils.ResUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 选集网格：一次只渲染当前分组（页）的内容，页内固定列宽，
 * 标签用 EpisodeInfo 解析出的「日期/集数 + 编码声道 + 清晰度角标」。
 */
public class EpisodeAdapter extends RecyclerView.Adapter<EpisodeAdapter.ViewHolder> {

    private final OnClickListener mListener;
    private final List<Episode> mItems;
    private EpisodeInfo.Plan mPlan;
    private int pageStart;
    private int pageEnd;
    private int nextFocusDown;
    private int nextFocusUp;

    public EpisodeAdapter(OnClickListener listener) {
        mListener = listener;
        mItems = new ArrayList<>();
        // GS-79：notifyDataSetChanged 把所有 ViewHolder 标 INVALID；未被复用的 scrap 会在
        // dispatchLayoutStep3.removeAndRecycleScrapInt 被清除，若含焦点格 → clearFocus →
        // rootViewRequestFocus，把焦点挑给布局最靠前的 video。稳定 id 让 INVALID 的 scrap
        // 可按 getItemId 复用，焦点格不进「未复用」集合，从机制上不发生上述抢焦点链。
        // 必须在注册 observer 之前（构造器内）设置，否则抛 IllegalStateException。
        setHasStableIds(true);
    }

    /**
     * 稳定 id = 页内槽位 position（不是 pageStart + position 的绝对索引）。
     * 本 adapter 的数据模型是「当前页 N 个槽位」：换页只是把同一批槽位重新绑定到不同集，
     * 槽位 id 必须跨页稳定，notifyDataSetChanged 后 scrap 里的焦点格才能按 id 被复用、留在视图树。
     * 绝对索引会让跨页后所有 id 全变、scrap 全部作废被清除，反而必然丢焦点（实测复现）。
     */
    @Override
    public long getItemId(int position) {
        return position;
    }

    public void addAll(List<Episode> items) {
        mItems.clear();
        mItems.addAll(items);
        List<String> names = new ArrayList<>();
        for (Episode item : items) names.add(item.getDesc() + " " + item.getName());
        mPlan = EpisodeInfo.analyze(names);
        if (com.fongmi.android.tv.BuildConfig.DEBUG) {
            StringBuilder sb = new StringBuilder();
            for (EpisodeInfo.Item item : mPlan.items) sb.append(item.label).append("|").append(item.tail).append("|").append(item.badge).append("  ");
            android.util.Log.d("QBoxEpisodes", String.format("count=%d cols=%d rows=%d group=%d groups=%d labels=%s", names.size(), mPlan.columns, mPlan.rows, mPlan.groupSize, mPlan.groupCount(), sb));
            android.util.Log.d("QBoxEpisodesRaw", names.toString());
        }
        pageStart = 0;
        pageEnd = mItems.size();
        notifyDataSetChanged();
    }

    public void clear() {
        mItems.clear();
        pageStart = 0;
        pageEnd = 0;
        notifyDataSetChanged();
    }

    /** 只展示当前分组（页）的区间；itemCount 变成页大小，onBind 用 pageStart 映射回完整列表。 */
    public void setPage(int start, int end) {
        start = Math.max(0, Math.min(start, mItems.size()));
        end = Math.max(start, Math.min(end, mItems.size()));
        if (pageStart == start && pageEnd == end) return;
        pageStart = start;
        pageEnd = end;
        notifyDataSetChanged();
    }

    public int getPageSize() {
        return Math.max(0, pageEnd - pageStart);
    }

    public int getTotalCount() {
        return mItems.size();
    }

    public EpisodeInfo.Plan getPlan() {
        return mPlan;
    }

    public int getColumns() {
        return mPlan == null ? 1 : mPlan.columns;
    }

    /** 某一组（页）实际条数，最后一组可能不满。 */
    public int getGroupSize(int groupIndex) {
        if (mPlan == null) return 0;
        int start = groupIndex * mPlan.groupSize;
        if (start >= mItems.size()) return 0;
        return Math.min(mPlan.groupSize, mItems.size() - start);
    }

    public int getPosition() {
        for (int i = 0; i < mItems.size(); i++) if (mItems.get(i).isSelected()) return i;
        return 0;
    }

    public int getLocalPosition() {
        int position = getPosition();
        return position >= pageStart && position < pageEnd ? position - pageStart : -1;
    }

    /** 就地刷新选中态用：某页内格对应的 Episode 是否为当前播放集。
     *  避免 renderEpisodeSelection 走 notifyDataSetChanged 全量重建 ViewHolder 而丢焦点（GS-78）。 */
    public boolean isSelectedAt(int position) {
        int index = pageStart + position;
        return index >= 0 && index < mItems.size() && mItems.get(index).isSelected();
    }

    // nextFocus 仅存值；实际重绑由 VideoActivity.updateFocus 统一 notifyItemChanged 触发（避免重复全量刷新）。
    public void setNextFocusDown(int nextFocusDown) {
        this.nextFocusDown = nextFocusDown;
    }

    public void setNextFocusUp(int nextFocusUp) {
        this.nextFocusUp = nextFocusUp;
    }

    @Override
    public int getItemCount() {
        return getPageSize();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterEpisodeBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        int index = pageStart + position;
        Episode item = mItems.get(index);
        EpisodeInfo.Item info = mPlan == null ? null : mPlan.items.get(index);
        int columns = Math.max(1, getColumns());
        int row = position / columns;
        boolean topRow = row == 0;
        boolean bottomRow = position + columns >= getPageSize();
        View root = holder.binding.getRoot();
        // 格间距；外层只负责焦点命中，真正的选中样式在文字胶囊上。
        ViewGroup.LayoutParams params = root.getLayoutParams();
        if (params instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams margins = (ViewGroup.MarginLayoutParams) params;
            margins.setMarginEnd(ResUtil.dp2px(10));
            margins.bottomMargin = ResUtil.dp2px(8);
            root.setLayoutParams(margins);
        }
        if (info == null) {
            holder.binding.prefix.setText(item.getDesc());
            holder.binding.text.setText(item.getName());
            holder.binding.badge.setVisibility(View.GONE);
            holder.binding.channel.setVisibility(View.GONE);
        } else {
            // 没有识别出日期/集数时，EpisodeInfo 会把整段清理后的文件名当成 label、rest 为空。
            // 此时若仍把 label 放在固定前缀里，长资源名会被前缀宽度截断且永远不会进入 marquee。
            // 把这种「无结构长名」移到可滚动 text，前缀只保留源的 desc（通常为空/集号）。
            String tail = info.codecTail();
            String scrollName = info.rest.isEmpty() ? info.label : info.rest;
            if (!tail.isEmpty()) scrollName = scrollName + (scrollName.isEmpty() ? "" : " ") + tail;
            String fixedPrefix = info.rest.isEmpty() && tail.isEmpty() ? item.getDesc() : info.label;
            holder.binding.prefix.setText(fixedPrefix);
            holder.binding.text.setText(scrollName);
            // 右上两排：分辨率 / 动态范围（DV·HDR）。
            String top = info.resolution() + (info.dynamicRange().isEmpty() ? "" : "\n" + info.dynamicRange());
            holder.binding.badge.setText(top);
            holder.binding.badge.setVisibility(top.isEmpty() ? View.GONE : View.VISIBLE);
            // 右下固定：声道 5.1/7.1 + ATMOS。
            String channel = info.channel();
            holder.binding.channel.setText(channel);
            holder.binding.channel.setVisibility(channel.isEmpty() ? View.GONE : View.VISIBLE);
        }
        applyPillPadding(holder);
        // 每一排只有首行向上跳出网格、末行向下跳出网格；左右由详情页显式路由。
        root.setNextFocusUpId(topRow ? nextFocusUp : 0);
        root.setNextFocusDownId(bottomRow ? nextFocusDown : 0);
        // 焦点/选中态由 root 承载，pill 复制父状态实现白药丸；text 不能复制父状态，
        // 否则 TextView 的 selected 会被父容器覆盖，marquee 不会启动。
        root.setActivated(item.isSelected());
        // 聚焦时 selected=true 只驱动 text（原资源全名）marquee 滚动，前缀 prefix 保持静止。
        root.setOnFocusChangeListener((v, hasFocus) -> holder.binding.text.setSelected(hasFocus));
        holder.binding.text.setSelected(root.hasFocus());
        root.setOnClickListener(v -> mListener.onItemClick(item));
    }

    /** 固定大小按钮内「文字内边距」的唯一来源：作用于 pill（胶囊容器），覆盖 shape_item_* 自带的
     *  <padding>，避免 shape + XML + 代码三处叠加。右侧按 badge / channel 角标实测最大宽留白，
     *  让角标钉在右上/右下、压不到滚动文字（GS-70）。 */
    private void applyPillPadding(ViewHolder holder) {
        int left = ResUtil.dp2px(12);
        int top = ResUtil.dp2px(8);
        int right = ResUtil.dp2px(12);
        int bottom = ResUtil.dp2px(8);
        int unspec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        int badgeW = 0;
        if (holder.binding.badge.getVisibility() == View.VISIBLE) {
            holder.binding.badge.measure(unspec, unspec);
            badgeW = holder.binding.badge.getMeasuredWidth();
        }
        int channelW = 0;
        if (holder.binding.channel.getVisibility() == View.VISIBLE) {
            holder.binding.channel.measure(unspec, unspec);
            channelW = holder.binding.channel.getMeasuredWidth();
        }
        int corner = Math.max(badgeW, channelW);
        if (corner > 0) right += corner + ResUtil.dp2px(2);
        holder.binding.pill.setPadding(left, top, right, bottom);
    }

    public interface OnClickListener {

        void onItemClick(Episode item);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterEpisodeBinding binding;

        ViewHolder(@NonNull AdapterEpisodeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
