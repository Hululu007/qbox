package com.fongmi.android.tv.ui.adapter;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.bean.Group;
import com.fongmi.android.tv.databinding.AdapterGroupBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.ViewHolder> {

    private final OnClickListener mListener;
    private final List<Group> mItems;

    public GroupAdapter(OnClickListener listener) {
        mListener = listener;
        mItems = new ArrayList<>();
    }

    public void addAll(List<Group> items) {
        mItems.clear();
        mItems.addAll(items);
        notifyDataSetChanged();
    }

    public void add(int position, Group item) {
        mItems.add(position, item);
        notifyItemInserted(position);
    }

    public void clear() {
        mItems.clear();
        notifyDataSetChanged();
    }

    public Group get(int position) {
        return mItems.get(position);
    }

    public int indexOf(Group item) {
        return mItems.indexOf(item);
    }

    public List<Group> unmodifiableList() {
        return Collections.unmodifiableList(mItems);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterGroupBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Group item = mItems.get(position);
        holder.binding.name.setText(item.getName());
        // 褰撳墠鍒嗙粍鐢ㄥ姞绮楁爣璁帮細Leanback 鐨?selected 浼氳窡鐫€鐒︾偣璧帮紝涓嶈兘鐢ㄨ儗鏅尯鍒嗐€屾鍦ㄧ湅鍝釜鍒嗙粍銆嶃€?
        holder.binding.name.setTypeface(null, item == current ? Typeface.BOLD : Typeface.NORMAL);
        holder.binding.getRoot().setOnClickListener(v -> mListener.onItemClick(item));
    }

    /** 褰撳墠鎾斁鐨勫垎缁勶紝浠呯敤浜庡姞绮楁爣璁帮紝涓嶅弬涓庣劍鐐?閫変腑鐘舵€併€?*/
    private Group current;

    public void setCurrent(Group group) {
        if (current == group) return;
        current = group;
        notifyDataSetChanged();
    }

    public interface OnClickListener {
        void onItemClick(Group item);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterGroupBinding binding;

        ViewHolder(@NonNull AdapterGroupBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
