package com.fongmi.android.tv.ui.holder;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.AdapterVodRectBinding;
import com.fongmi.android.tv.ui.base.BaseVodHolder;
import com.fongmi.android.tv.ui.presenter.VodPresenter;
import com.fongmi.android.tv.utils.ImgUtil;

public class VodRectHolder extends BaseVodHolder {

    private final VodPresenter.OnClickListener listener;
    private final AdapterVodRectBinding binding;

    public VodRectHolder(@NonNull AdapterVodRectBinding binding, VodPresenter.OnClickListener listener) {
        super(binding.getRoot());
        this.binding = binding;
        this.listener = listener;
    }

    public VodRectHolder size(int[] size) {
        binding.image.getLayoutParams().height = size[1];
        binding.getRoot().getLayoutParams().width = size[0];
        return this;
    }

    @Override
    public void initView(Vod item) {
        String meta = getMeta(item);
        binding.name.setText(item.getName());
        binding.name.setVisibility(item.getNameVisible());
        binding.site.setText(item.getSiteName());
        binding.site.setVisibility(item.getSiteVisible());
        binding.year.setVisibility(android.view.View.GONE);
        binding.remark.setText(meta);
        binding.remark.setVisibility(TextUtils.isEmpty(meta) ? android.view.View.GONE : android.view.View.VISIBLE);
        // 海报态（更新推荐等）标题可能 GONE，焦点卡没有可读文本，TalkBack 静默（N-48）；
        // 根节点补上内容名：有标题读标题，没有则退到备注/站点名。
        binding.getRoot().setContentDescription(buildDesc(item, meta));
        binding.getRoot().setOnClickListener(v -> listener.onItemClick(item));
        binding.getRoot().setOnLongClickListener(v -> listener.onLongClick(item));
        ImgUtil.load(item.getName(), item.getPic(), binding.image);
    }

    private CharSequence buildDesc(Vod item, String meta) {
        if (!TextUtils.isEmpty(item.getName())) return item.getName();
        if (!TextUtils.isEmpty(meta)) return meta;
        return item.getSiteName();
    }

    /** 年份与备注合并成一行次要信息（2026 · 更新至第4集），卡片上不放彩色角标。 */
    private String getMeta(Vod item) {
        String year = item.getYearVisible() == android.view.View.VISIBLE ? item.getYear() : "";
        String remark = item.getRemarkVisible() == android.view.View.VISIBLE && isUsefulRemark(item.getRemarks()) ? item.getRemarks() : "";
        if (TextUtils.isEmpty(year)) return TextUtils.isEmpty(remark) ? "" : remark.trim();
        if (TextUtils.isEmpty(remark)) return year.trim();
        return year.trim() + " · " + remark.trim();
    }

    @Override
    public void unbind() {
        Glide.with(binding.image).clear(binding.image);
    }

    /** 评分 0、纯数字评分（如「7.0」）或空备注对用户没有信息量，直接不显示。 */
    static boolean isUsefulRemark(String remark) {
        if (remark == null) return false;
        String s = remark.trim();
        if (s.isEmpty() || s.equals("0") || s.equals("0.0")) return false;
        if (s.matches("^\\d+(\\.\\d+)?$")) return false;
        if (s.matches("^\\d+(\\.\\d+)?\\s*[分點点]$")) return false;
        return !(s.startsWith("评分：") || s.startsWith("评分:0") || s.startsWith("评分：0") || s.startsWith("评分:0.0"));
    }
}
