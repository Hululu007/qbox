package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.AdapterEpisodeGroupBinding;
import com.fongmi.android.tv.databinding.AdapterEpisodeSortBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * 选集分组条：0 号是「正序 / 倒序」小标志，后面才是各组（1-21 / 22-42 …）。
 * 分组只做位置跳转，不裁剪完整选集列表。
 */
public class EpisodeGroupAdapter extends RecyclerView.Adapter<EpisodeGroupAdapter.ViewHolder> {

    /** 0 号是排序标志，分组从 1 号开始。 */
    public static final int OFFSET = 1;

    private static final int TYPE_SORT = 0;
    private static final int TYPE_GROUP = 1;

    private final OnClickListener mListener;
    private final List<Group> mItems;
    private int mSelected = -1;
    private boolean mDesc;
    private int mNextFocusUp;
    private int mNextFocusDown;

    public EpisodeGroupAdapter(OnClickListener listener) {
        mListener = listener;
        mItems = new ArrayList<>();
    }

    public interface OnClickListener {

        void onGroupClick(Group item);

        void onSortClick();
    }

    public void addAll(List<Group> items) {
        mItems.clear();
        mItems.addAll(items);
        mSelected = mItems.isEmpty() ? -1 : 0;
        notifyDataSetChanged();
    }

    public void setDesc(boolean desc) {
        if (mDesc == desc) return;
        mDesc = desc;
        notifyItemChanged(0);
    }

    public int getGroupCount() {
        return mItems.size();
    }

    public Group get(int groupIndex) {
        return mItems.get(groupIndex);
    }

    public int getSelected() {
        return Math.max(0, mSelected);
    }

    public int indexOf(int episodePosition) {
        for (int i = 0; i < mItems.size(); i++) {
            Group item = mItems.get(i);
            if (episodePosition >= item.getStart() && episodePosition < item.getEnd()) return i;
        }
        return -1;
    }

    public void setSelected(int groupIndex) {
        if (mSelected == groupIndex) return;
        int old = mSelected;
        mSelected = groupIndex;
        if (old >= 0) notifyItemChanged(old + OFFSET);
        if (groupIndex >= 0) notifyItemChanged(groupIndex + OFFSET);
    }

    public void setNextFocusUp(int nextFocusUp) {
        mNextFocusUp = nextFocusUp;
    }

    public void setNextFocusDown(int nextFocusDown) {
        mNextFocusDown = nextFocusDown;
    }

    @Override
    public int getItemCount() {
        return mItems.size() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? TYPE_SORT : TYPE_GROUP;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        TextView view = viewType == TYPE_SORT
                ? AdapterEpisodeSortBinding.inflate(inflater, parent, false).getRoot()
                : AdapterEpisodeGroupBinding.inflate(inflater, parent, false).getRoot();
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TextView text = holder.text;
        text.setNextFocusUpId(mNextFocusUp);
        text.setNextFocusDownId(mNextFocusDown);
        if (position == 0) {
            text.setText(mDesc ? R.string.play_sort_desc : R.string.play_sort_asc);
            text.setOnClickListener(v -> mListener.onSortClick());
            return;
        }
        Group item = mItems.get(position - 1);
        text.setText(item.getLabel());
        text.setSelected(position - 1 == mSelected);
        text.setOnClickListener(v -> {
            setSelected(position - 1);
            mListener.onGroupClick(item);
        });
    }

    public static class Group {

        private final String label;
        private final int start;
        private final int end;

        public Group(String label, int start, int end) {
            this.label = label;
            this.start = start;
            this.end = end;
        }

        public String getLabel() {
            return label;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView text;

        ViewHolder(@NonNull TextView text) {
            super(text);
            this.text = text;
        }
    }
}
