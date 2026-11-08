package com.fongmi.android.tv.ui.custom;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.leanback.widget.FocusHighlight;
import androidx.leanback.widget.HorizontalGridView;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.RowPresenter;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.utils.ResUtil;

/**
 * 行容器：分组标题（HeaderItem）直接画在卡片行上方，标题与卡片行是同一个 view。
 *
 * <p>为什么不用「标题单独一行」：分组标题作为独立行插在列表里之后，
 * Leanback 的对齐目标是「选中行顶部贴 paddingTop」，标题会被留在对齐线之上、
 * 裁剪线之下 —— 于是要靠滚动结束后的 snap 校正把标题拉回可视区。
 * 而 snap 的滚动恰好打断 Leanback「滚动对齐结束后再落焦点」的流程，
 * 表现就是按下键后焦点钉死在旧行、画面纹丝不动（焦点与选位分裂）。
 * 标题进了行内之后，Leanback 原生对齐天然把标题带到可视区，
 * snap 校正在首页的偏差归零（dy≤4 直接返回，不再发起滚动），冲突根除。
 *
 * <p>leanback 1.2.0 的 ListRowView 本身就是纵向 LinearLayout、只含一个 grid 子视图
 * （lb_list_row.xml 里 grid 是 match_parent / wrap_content），
 * 所以标题直接 addView(index=0) 即可，不需要改任何布局参数。
 *
 * <p>没有 HeaderItem 的行（分组的后续行）标题区收起，不留空隙。
 */
public class CustomRowPresenter extends ListRowPresenter {

    private static final int TITLE_ID = R.id.qbox_row_title;
    private static final int TITLE_LAYOUT = R.layout.adapter_header;

    private final int spacing;
    private final int focusZoomFactor;

    public CustomRowPresenter(int spacing) {
        this(spacing, FocusHighlight.ZOOM_FACTOR_SMALL);
    }

    public CustomRowPresenter(int spacing, int focusZoomFactor) {
        super(focusZoomFactor);
        this.spacing = spacing;
        this.focusZoomFactor = focusZoomFactor;
        setShadowEnabled(false);
        setSelectEffectEnabled(false);
        setKeepChildForeground(false);
        // leanback 默认自带 RowHeaderPresenter，会把一个小号灰字标题（RowHeaderView）
        // 包在 RowContainerView 里放在行上方 —— 与行内标题重复显示（「最近观看」出现两次）。
        // 关掉自带标题，行里只剩我们自己画的分组标题。
        setHeaderPresenter(null);
    }

    @Override
    protected RowPresenter.ViewHolder createRowViewHolder(@NonNull ViewGroup parent) {
        RowPresenter.ViewHolder holder = super.createRowViewHolder(parent);
        // 1.9.16 重写行容器时漏掉了横向间距：卡片之间贴在一起（海报墙看起来像一张拼图），
        // 历史卡比海报宽时还会压到下一张的片名上。这里把构造参数里的 spacing 真正接上。
        if (holder instanceof ListRowPresenter.ViewHolder) {
            HorizontalGridView gridView = ((ListRowPresenter.ViewHolder) holder).getGridView();
            gridView.setHorizontalSpacing(ResUtil.dp2px(spacing));
            // 筛选 chip 行（ZOOM_FACTOR_NONE）紧凑化：leanback 默认行上下各 8dp，
            // 行间 16dp 对大 chip 观感太松；收到各 4dp（行间 8dp，守 8dp 栅格）。
            if (focusZoomFactor == FocusHighlight.ZOOM_FACTOR_NONE) {
                gridView.setPadding(gridView.getPaddingLeft(), ResUtil.dp2px(4), gridView.getPaddingRight(), ResUtil.dp2px(4));
                // 第 32 轮：filter 子行左右死端钳制——行首再按 ← / 行尾再按 →（含长按 repeat，每次 repeat
                // 都是一次新的 ACTION_DOWN，同样被拦）焦点不飞出本行；换大 tab 只能走 tab 行。
                // UP/DOWN 不拦截，仍按设计进入相邻行。实测旧行为：行尾长按 → 飞出→海报行→触发 ViewPager
                // 方向键翻页，连翻几个大 tab（第 32 轮改前证据 r32_esc2/esc3.png）。
                gridView.setOnKeyInterceptListener(event -> {
                    if (event.getAction() != android.view.KeyEvent.ACTION_DOWN) return false;
                    int key = event.getKeyCode();
                    if (key != android.view.KeyEvent.KEYCODE_DPAD_LEFT && key != android.view.KeyEvent.KEYCODE_DPAD_RIGHT) return false;
                    android.view.View focusedChild = gridView.getFocusedChild();
                    if (focusedChild == null) return false;
                    int pos = gridView.getChildAdapterPosition(focusedChild);
                    int count = gridView.getAdapter() == null ? 0 : gridView.getAdapter().getItemCount();
                    if (key == android.view.KeyEvent.KEYCODE_DPAD_LEFT) return pos <= 0;
                    return pos >= count - 1;
                });
            }
        }
        View root = holder.view;
        if (root instanceof LinearLayout) {
            LinearLayout row = (LinearLayout) root;
            // ListRowView 构造里已 setOrientation(VERTICAL)，这里再写一次只是显式声明意图。
            row.setOrientation(LinearLayout.VERTICAL);
            // 行宽必须 MATCH_PARENT：ItemBridgeAdapter 挂载行视图时用的是 RecyclerView
            // 默认的 WRAP_CONTENT 宽度，而标题（较窄）排在 index 0 先测量，
            // 内部 match_parent 的网格会被量成标题的宽度 ——
            // 宽卡片在窄网格里居中溢出，左半截被屏幕边缘裁掉。
            // 注意此时视图还没挂进父容器、getLayoutParams() 为 null，
            // 所以必须直接 setLayoutParams，不能只在旧参数上改。
            row.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            row.addView(createTitle(parent), 0);
            // 同理，网格宽度也钉死为 MATCH_PARENT，不依赖 leanback 布局里的默认值。
            for (int i = 0; i < row.getChildCount(); i++) {
                View child = row.getChildAt(i);
                if (child instanceof HorizontalGridView) {
                    ViewGroup.LayoutParams gp = child.getLayoutParams();
                    if (gp.width != ViewGroup.LayoutParams.MATCH_PARENT) {
                        gp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                        child.setLayoutParams(gp);
                    }
                }
            }
        }
        return holder;
    }

    /** 复用 adapter_header.xml，保证与其它页面的分组标题字号/字重/字距完全一致。 */
    private View createTitle(ViewGroup parent) {
        View title = LayoutInflater.from(parent.getContext()).inflate(TITLE_LAYOUT, parent, false);
        title.setId(TITLE_ID);
        title.setVisibility(View.GONE);
        title.setFocusable(false);
        title.setFocusableInTouchMode(false);
        title.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        // 标题贴在卡片行上方，但必须留出「聚焦放大」的垂直余量：
        // 卡片聚焦时以中心放大（约 +10%），顶边向上溢出行高 5.5% 左右，
        // 没有这段边距的话放大后的卡片会直接压在标题下半截上（真机实测复现）。
        // 用 padding 而不是 layout_margin：ListRowView 在 addView(child, 0) 时
        // 会用自己的 LayoutParams 覆盖掉 margin，margin 实测丢失（标题被卡片压住）。
        int gap = ResUtil.dp2px(18);
        title.setPadding(title.getPaddingLeft(), title.getPaddingTop(), title.getPaddingRight(), gap);
        if (title.getLayoutParams() instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) title.getLayoutParams();
            lp.topMargin = 0;
            lp.bottomMargin = 0;
            title.setLayoutParams(lp);
        }
        return title;
    }

    @Override
    protected void onBindRowViewHolder(@NonNull RowPresenter.ViewHolder holder, @Nullable Object item) {
        super.onBindRowViewHolder(holder, item);
        TextView title = holder.view.findViewById(TITLE_ID);
        if (title == null) return;
        CharSequence text = item instanceof ListRow && ((ListRow) item).getHeaderItem() != null
                ? ((ListRow) item).getHeaderItem().getName() : null;
        title.setText(TextUtils.isEmpty(text) ? "" : text);
        title.setVisibility(TextUtils.isEmpty(text) ? View.GONE : View.VISIBLE);
    }
}
