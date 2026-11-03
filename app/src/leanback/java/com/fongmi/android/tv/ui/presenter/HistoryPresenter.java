package com.fongmi.android.tv.ui.presenter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.leanback.widget.Presenter;

import com.bumptech.glide.Glide;
import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.databinding.AdapterHistoryBinding;
import com.fongmi.android.tv.utils.ImgUtil;
import com.fongmi.android.tv.utils.ResUtil;

public class HistoryPresenter extends Presenter {

    private final OnClickListener listener;
    private int width, height;
    private boolean delete;

    public HistoryPresenter(OnClickListener listener) {
        this.listener = listener;
        setLayoutSize();
    }

    public interface OnClickListener {

        void onItemClick(History item);

        void onItemDelete(History item);

        boolean onLongClick();
    }

    private void setLayoutSize() {
        int space = ResUtil.dp2px(64) + ResUtil.dp2px(16 * (Product.getColumn() - 1));
        int base = ResUtil.getScreenWidth() - space;
        width = base / Product.getColumn();
        height = (int) (width * 9f / 16f);
    }

    public boolean isDelete() {
        return delete;
    }

    public void setDelete(boolean delete) {
        this.delete = delete;
    }

    private void setClickListener(View root, History item) {
        root.setOnLongClickListener(view -> listener.onLongClick());
        root.setOnClickListener(view -> {
            if (isDelete()) listener.onItemDelete(item);
            else listener.onItemClick(item);
        });
    }

    @NonNull
    @Override
    public Presenter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent) {
        ViewHolder holder = new ViewHolder(AdapterHistoryBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        holder.binding.getRoot().getLayoutParams().width = width;
        holder.binding.image.getLayoutParams().height = height;
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull Presenter.ViewHolder viewHolder, Object object) {
        History item = (History) object;
        boolean same = item.getVodName().equals(item.getVodRemarks()) || isNumeric(item.getVodRemarks());
        ViewHolder holder = (ViewHolder) viewHolder;
        setClickListener(holder.view, item);
        // onCreateViewHolder 里设的宽度会在挂到 Leanback 网格时被网格自己的 LayoutParams 覆盖，
        // 结果历史卡按文字宽度被撑到两倍，相邻两张卡的片名叠在一起（实测「后会友期」压在上一张卡的片名上）。
        // 最可靠的做法是把每个子 View 的宽度都钉死：root 是 wrap_content，子 View 一旦是 match_parent，
        // 就会被文字重新撑宽。绑定时统一改成固定 px，root 再窄也溢不出去。
        if (holder.binding.image.getLayoutParams() != null) {
            holder.binding.image.getLayoutParams().width = width;
            holder.binding.image.getLayoutParams().height = height;
        }
        if (holder.binding.name.getLayoutParams() != null) holder.binding.name.getLayoutParams().width = width;
        if (holder.binding.remark.getLayoutParams() != null) holder.binding.remark.getLayoutParams().width = width;
        if (holder.view.getLayoutParams() != null) holder.view.getLayoutParams().width = width;
        holder.view.requestLayout();
        holder.binding.name.setText(item.getVodName());
        holder.binding.site.setText(item.getSiteName());
        holder.binding.remark.setText(item.getVodRemarks());
        holder.binding.site.setVisibility(item.getSiteVisible());
        holder.binding.delete.setVisibility(!delete ? View.GONE : View.VISIBLE);
        holder.binding.remark.setVisibility(delete || same ? View.GONE : View.VISIBLE);
        boolean hasProgress = !delete && item.getDuration() > 0 && item.getPosition() > 0;
        holder.binding.progress.setVisibility(hasProgress ? View.VISIBLE : View.GONE);
        if (hasProgress) {
            int percent = (int) Math.min(1000, Math.max(0, item.getPosition() * 1000 / item.getDuration()));
            holder.binding.progress.setProgress(percent);
        }
        ImgUtil.load(item.getVodName(), item.getVodPic(), holder.binding.image);
    }

    @Override
    public void onUnbindViewHolder(@NonNull Presenter.ViewHolder viewHolder) {
        ViewHolder holder = (ViewHolder) viewHolder;
        Glide.with(holder.binding.image).clear(holder.binding.image);
    }

    /** 纯数字（集数/评分）对卡片没有信息量，直接不显示。 */
    private static boolean isNumeric(String text) {
        return text != null && text.trim().matches("^\\d+(\\.\\d+)?$");
    }

    public static class ViewHolder extends Presenter.ViewHolder {

        private final AdapterHistoryBinding binding;

        public ViewHolder(@NonNull AdapterHistoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}