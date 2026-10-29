package com.fongmi.android.tv.ui.presenter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.leanback.widget.Presenter;

import com.fongmi.android.tv.bean.Func;
import com.fongmi.android.tv.databinding.AdapterFuncTileBinding;

/** 首页功能入口（直播/搜索/收藏/推送/设置），对齐 OK影视 的方块卡片。 */
public class FuncTilePresenter extends Presenter {

    private final FuncPresenter.OnClickListener listener;

    public FuncTilePresenter(FuncPresenter.OnClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Presenter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent) {
        return new ViewHolder(AdapterFuncTileBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Presenter.ViewHolder viewHolder, Object object) {
        Func item = (Func) object;
        ViewHolder holder = (ViewHolder) viewHolder;
        holder.binding.text.setText(item.getText());
        if (item.getDrawable() == 0) {
            holder.binding.icon.setVisibility(View.GONE);
        } else {
            holder.binding.icon.setVisibility(View.VISIBLE);
            holder.binding.icon.setImageResource(item.getDrawable());
        }
        setOnClickListener(holder, view -> listener.onItemClick(item));
        holder.binding.getRoot().setOnLongClickListener(view -> listener.onItemLongClick(item));
    }

    @Override
    public void onUnbindViewHolder(@NonNull Presenter.ViewHolder viewHolder) {
    }

    public static class ViewHolder extends Presenter.ViewHolder {

        private final AdapterFuncTileBinding binding;

        public ViewHolder(@NonNull AdapterFuncTileBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
