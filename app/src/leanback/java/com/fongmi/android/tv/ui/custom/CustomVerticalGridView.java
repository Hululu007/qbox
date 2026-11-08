package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.leanback.widget.HorizontalGridView;
import androidx.leanback.widget.OnChildViewHolderSelectedListener;
import androidx.leanback.widget.VerticalGridView;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.BuildConfig;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.utils.KeyUtil;
import com.fongmi.android.tv.utils.ResUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class CustomVerticalGridView extends VerticalGridView {

    private List<View> views;
    private boolean pressDown;
    private boolean pressUp;
    private boolean moveTop;
    private boolean hideHeaderEnabled = true;

    /** 内容区顶边（顶栏 + 分类条下沿），px。低于这条线的内容一律不画。 */
    private int contentTop = -1;
    /** 裁剪处的渐隐高度，px。只柔化切换，绝不越过第一行顶边。 */
    private int fadeHeight;
    /** 是否启用「动态裁剪到当前行顶边」。 */
    private boolean alignTop;
    /** 正在播放贴顶校正动画。校正期间不再叠加新的校正，防止抖动。 */
    private boolean snapAnimating;
    /** 聚焦放大的垂直溢出量（行高 × 5.5%），裁剪线必须让开这么多。 */
    private int zoomPad;
    /** 顶栏 + 分类条的下沿。裁剪线最多只能下探到这里，再往上就会盖住分类条。 */
    private int headerBottom = -1;
    /** 从这个位置起才做「滚动贴顶」。筛选项这类需要多行同时可见的行不参与。 */
    private int snapStartIndex;
    /** 当前行高（px）与行距（px），用于推导底部留白与裁剪线。 */
    private int rowHeight;
    private int rowSpacing;
    /** 上一次贴顶校正的偏差与连续尝试次数，用来打断「滚不动还一直重试」的死循环。 */
    private int lastSnapDy;
    private int snapTries;
    /** 筛选面板展开时关掉贴顶校正：面板要固定在上方，不能被逐行顶出去。 */
    private boolean snapEnabled = true;

    public CustomVerticalGridView(@NonNull Context context) {
        this(context, null);
    }

    public CustomVerticalGridView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomVerticalGridView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setMoveTop(true);
        // 内容不足一屏时（分类只有几条）默认会垂直居中，上面空一大片、海报贴着底部。
        // 统一从内容区顶部往下排，任何数据量下第一行位置都一致。
        setGravity(Gravity.TOP);
        // Leanback 默认把「选中项的垂直中心」作为对齐点，配合 WINDOW_ALIGN_LOW_EDGE 时
        // 会让行顶跑到 keyLine - 行高/2（实测差 306px）。改成对齐项的上边缘，
        // 行顶才会精确落在 windowAlignmentOffset（= 内容区起点）上。
        setItemAlignmentOffset(0);
        setItemAlignmentOffsetPercent(ITEM_ALIGN_OFFSET_PERCENT_DISABLED);
    }

    @Override
    protected void initAttributes(@NonNull Context context, @Nullable AttributeSet attrs) {
        super.initAttributes(context, attrs);
        setOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(@NonNull RecyclerView parent, @Nullable ViewHolder child, int position, int subposition) {
                lastSnapDy = 0;
                snapTries = 0;
                if (hideHeaderEnabled && pressDown && position == 1) hideHeader();
                if (hideHeaderEnabled && pressUp && position == 0) showHeader();
            }
        });
        addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState != RecyclerView.SCROLL_STATE_IDLE) return;
                // 校正动画结束时也在这里落地：解除标记后核验一次（动画期间布局可能又变了）。
                // dy ≤ 4 时 snapOnce 自己会停，所以不会成环。
                snapAnimating = false;
                requestSnap();
            }
        });
    }

    /**
     * 聚合搜索页不要收顶部标题/站点 tab：收了以后 pager 往上顶，
     * 第一排海报会贴到工具栏下面，焦点框上沿被裁掉。
     */
    public void setHideHeaderEnabled(boolean enabled) {
        this.hideHeaderEnabled = enabled;
    }

    public void setHeader(FragmentActivity activity, int... layoutIds) {
        if (activity != null) views = Arrays.stream(layoutIds).mapToObj(id -> (View) activity.findViewById(id)).filter(Objects::nonNull).toList();
    }

    public void setMoveTop(boolean moveTop) {
        this.moveTop = moveTop;
    }

    /**
     * 临时压低内容区起点（筛选面板展开时用）：让第一排筛选项贴近 tab，
     * 不用改全局 144dp 的内容基线。传负数恢复。
     * 垫层固定 4dp：不能用海报行的 zoomPad（筛选 chip 不放大，zoomPad 会把间距撑大 2-3 倍）。
     */
    public void setContentTopTemp(int topPx) {
        if (topPx < 0) return;
        this.contentTop = topPx;
        setPadding(getPaddingLeft(), topPx + ResUtil.dp2px(4), getPaddingRight(), getPaddingBottom());
        applyBottomPadding();
        requestLayout();
        invalidate();
    }

    /** 从第几行开始做滚动贴顶（筛选项行传筛选行数，普通列表传 0）。 */
    public void setSnapStartIndex(int index) {
        this.snapStartIndex = Math.max(0, index);
    }

    /** 筛选面板展开/收起时整体开关贴顶（面板要保持固定，结果区不能把面板顶走）。 */
    public void setSnapEnabled(boolean enabled) {
        this.snapEnabled = enabled;
        this.lastSnapDy = 0;
        this.snapTries = 0;
    }

    /**
     * 切换「按行对齐」/「只保证可见」。
     * 筛选面板展开时用后者：选中项只在离开视口时才滚动，面板不会被逐行顶出去。
     */
    @android.annotation.SuppressLint("RestrictedApi")
    public void setRowsKeepVisible(boolean keepVisible) {
        setFocusScrollStrategy(keepVisible ? FOCUS_SCROLL_ITEM : FOCUS_SCROLL_ALIGNED);
    }

    /** 只设固定裁剪线：适合 header 不在浮层里、自己算好 padding 的页面。 */
    public void setClipTop(int clipTop) {
        this.contentTop = clipTop;
        this.fadeHeight = 0;
        alignTop = false;
        invalidate();
    }

    public void applyContentTop(int topPx, int rowHeightPx) {
        applyContentTop(topPx, rowHeightPx, true);
    }

    /**
     * QBox 统一的内容区顶部几何。
     *
     * @param topPx       内容区顶边（通常是 qbox_content_top）
     * @param rowHeightPx 一行实际高度（海报 + 标题区），用来推导行距和放大余量
     * @param zoom        卡片聚焦时是否放大。首页卡片不放大（焦点描边画在卡片内部），
     *                    把余量降到 4dp，上一行底边必然落在裁剪线以上，一点残影都留不下。
     */
    public void applyContentTop(int topPx, int rowHeightPx, boolean zoom) {
        applyContentTop(topPx, topPx, rowHeightPx, zoom);
    }

    /**
     * @param topPx        内容区起点（第一行在顶部时的位置基准）
     * @param bottomPx     顶栏 + 分类条下沿，裁剪线的硬下限（再往上会盖住分类条）
     * @param rowHeightPx  一行实际高度
     * @param zoom         聚焦时是否放大
     */
    public void applyContentTop(int topPx, int bottomPx, int rowHeightPx, boolean zoom) {
        this.contentTop = topPx;
        this.headerBottom = bottomPx;
        int zoomPad = zoom ? Math.max(ResUtil.dp2px(6), Math.round(rowHeightPx * 0.055f)) : ResUtil.dp2px(4);
        this.zoomPad = zoomPad;
        // 裁剪线正好落在「放大后卡片的上边缘」，那里本来是空的，不需要再淡出；
        // 加了渐隐反而会把放大后海报的顶边擦掉一条。
        this.fadeHeight = 0;
        int spacing = Math.max(ResUtil.dp2px(24), Math.round(rowHeightPx * 0.075f));
        if (spacing <= zoomPad) spacing = zoomPad + ResUtil.dp2px(4);
        this.rowHeight = rowHeightPx;
        this.rowSpacing = spacing;
        setVerticalSpacing(spacing);
        setPadding(getPaddingLeft(), topPx + zoomPad, getPaddingRight(), getPaddingBottom());
        // 让 Leanback 自己把选中行贴到内容区顶部：LOW_EDGE + 固定偏移（百分比关掉）。
        // 之前靠滚动结束后的 snap 补位移，会和 Leanback 的下一次布局抢滚动，
        // 表现是「按一下先窜过头，再慢慢滑回来」的弹簧感。
        setWindowAlignment(WINDOW_ALIGN_LOW_EDGE);
        setWindowAlignmentOffset(topPx + zoomPad);
        setWindowAlignmentOffsetPercent(WINDOW_ALIGN_OFFSET_PERCENT_DISABLED);
        // 行数少时（最后一两排）默认没有足够的滚动余量，选中行永远贴不到顶部：
        // 表现就是「按到底，上一排半截海报一直挂在那儿」，而且 requestSnap 每帧
        // 都发现 dy>0 却滚不动，形成空转。底部留出一屏减一行的空白，任何一行都能贴顶。
        applyBottomPadding();
        alignTop = true;
        requestLayout();
        invalidate();
        // 首帧没有滚动事件，补一次校正，避免 Leanback 把选中行居中后露出上一行。
        postDelayed(this::requestSnap, 240);
        post(this::applyBottomPadding);
    }

    /**
     * 列表已经在页头下面（聚合搜索页这种 LinearLayout 布局），沿用现有 paddingTop，
     * 只接上「行距 + 放大余量 + 滚动贴顶」，不额外改变内容起点。
     */
    public void applyContentTopKeepPadding(int rowHeightPx, boolean zoom) {
        int pad = zoom ? Math.max(ResUtil.dp2px(6), Math.round(rowHeightPx * 0.055f)) : ResUtil.dp2px(4);
        int top = getPaddingTop();
        applyContentTop(top - pad, Math.max(0, top - pad), rowHeightPx, zoom);
    }

    private void applyBottomPadding() {
        if (rowHeight <= 0 || getHeight() <= 0) return;
        int pad = Math.max(0, getHeight() - getPaddingTop() - rowHeight + rowSpacing);
        if (getPaddingBottom() == pad) return;
        setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), pad);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        applyBottomPadding();
    }

    /**
     * 裁剪线固定在内容区顶边，不跟着选中行跳。
     *
     * <p>曾经试过「裁到当前行顶边」——效果是上一行瞬间整行消失，滚动动画被吃掉，
     * 而且筛选项这种要同时看多行的场景会被裁得只剩一行。
     * 现在的做法是：裁剪线不动，让 Leanback 正常滚动把上一行送出去，
     * 滚动停下后再把选中行校正到内容区顶部（{@link #requestSnap()}）。
     */
    private int computeClipTop() {
        if (contentTop <= 0) return -1;
        // 选中行贴顶后，上一行的底边在「行顶 - 行距」处。裁剪线必须压在这条线之上，
        // 否则一行半截海报会一直挂在分类条下面（列表到底时最明显）。
        // 同时不能高过 headerBottom，否则会盖住分类条；也不能低于内容区顶边，
        // 否则会把贴顶那一行的分组标题切掉。
        int rowTop = getPaddingTop();
        int clip = rowTop - rowSpacing + ResUtil.dp2px(1);
        int min = headerBottom > 0 ? headerBottom : contentTop;
        return Math.max(min, Math.min(clip, rowTop));
    }

    /**
     * 滚动停下后把当前行贴到内容区顶部。
     *
     * <p>只做「位移校正」，不动滚动过程，所以主滚动动画是 Leanback 自己的。
     * 校正本身用 {@code smoothScrollBy} 平滑播放——曾经用 {@code scrollBy} 瞬移，
     * 用户看到的是「滚动结束后又咯噔跳一下」；也曾经用 lastSnapDy 防「同向二连发」，
     * 结果数据落位/图片撑高后同一偏差再次出现时校正被永久卡死，
     * 列表停在错误位置（zf1 里第一排比贴顶位置高出 22px 就是这么来的）。
     * 现在的收敛纪律：
     * <ol>
     *   <li>容差 4px —— 横向切换时行高会有 1~2px 的亚像素变化，容差太小等于每次都校正；</li>
     *   <li>校正动画期间（snapAnimating）不再叠加新的校正，IDLE 后再核验；</li>
     *   <li>校正到位后 dy 归 0，自然停止，不会自激振荡。</li>
     * </ol>
     */
    public void requestSnap() {
        post(this::snapOnce);
    }

    private void snapOnce() {
        if (!alignTop || !snapEnabled || getChildCount() == 0) return;
        int pos = getSelectedPosition();
        // 筛选面板要一次看三四行，贴顶会把它整个甩出去，所以它不参与。
        if (pos < snapStartIndex) return;
        View row = findChildByPosition(pos);
        if (row == null) return;
        int dy = row.getTop() - getPaddingTop();
        if (com.fongmi.android.tv.BuildConfig.DEBUG) {
            android.util.Log.d("QBoxSnap", "pos=" + pos + " rowTop=" + row.getTop()
                    + " paddingTop=" + getPaddingTop() + " dy=" + dy + " animating=" + snapAnimating
                    + " clip=" + computeClipTop() + " contentTop=" + contentTop + " zoomPad=" + zoomPad + " headerBottom=" + headerBottom);
        }
        if (Math.abs(dy) <= 4) {
            lastSnapDy = 0;
            snapTries = 0;
            return;
        }
        if (snapAnimating) return;
        // 滚动已经到底/到顶时 smoothScrollBy 不会位移，onScrollStateChanged(IDLE) 又会
        // 触发一次 requestSnap，同一偏差会被无限重试。Leanback 的 GridLayoutManager
        // 不实现 computeVerticalScrollRange()（实测恒为 0），不能用 canScrollVertically()
        // 判断，所以改成「同一个 dy 连续两次都没滚走就不再试」：正常的校正滚完后 dy 会变，
        // 只有滚不动的死循环会被拦下。
        if (snapTries > 0 && Math.abs(dy - lastSnapDy) <= 2) {
            lastSnapDy = dy;
            snapTries = 0;
            return;
        }
        lastSnapDy = dy;
        snapTries++;
        snapAnimating = true;
        smoothScrollBy(0, dy);
    }

    private View findChildByPosition(int position) {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child != null && getChildAdapterPosition(child) == position) return child;
        }
        return null;
    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        int clip = computeClipTop();
        if (clip < 0) {
            super.dispatchDraw(canvas);
            return;
        }
        int save = canvas.save();
        canvas.clipRect(0, clip, getWidth(), getHeight());
        super.dispatchDraw(canvas);
        canvas.restoreToCount(save);
        if (fadeHeight <= 0) return;
        // 只在裁剪处把列表自己的绘制淡出，背景从缺口透出来，避免切换瞬间出现硬切黑边。
        int layer = canvas.saveLayer(0, clip, getWidth(), clip + fadeHeight, null);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setShader(new LinearGradient(0, clip, 0, clip + fadeHeight,
                0xFFFFFFFF, 0x02FFFFFF, Shader.TileMode.CLAMP));
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
        canvas.drawRect(0, clip, getWidth(), clip + fadeHeight, paint);
        canvas.restoreToCount(layer);
    }

    public void hideHeader() {
        if (views != null) for (View view : views) view.setVisibility(View.GONE);
    }

    public void showHeader() {
        if (views != null) for (View view : views) view.setVisibility(View.VISIBLE);
    }

    public boolean isHeaderVisible() {
        if (views != null) for (View view : views) if (view.getId() == R.id.recycler && view.getVisibility() == View.VISIBLE) return true;
        return false;
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        if (!KeyUtil.isActionDown(event)) return super.dispatchKeyEvent(event);
        if (KeyUtil.isBackKey(event)) return moveTop && moveToTop();
        pressUp = KeyUtil.isUpKey(event);
        pressDown = KeyUtil.isDownKey(event);
        View before = findFocus();
        int selBefore = getSelectedPosition();
        boolean handled = super.dispatchKeyEvent(event);
        if (BuildConfig.DEBUG) {
            View after = findFocus();
            android.util.Log.d("QBoxGrid", "key=" + event.getKeyCode()
                    + " handled=" + handled
                    + " sel " + selBefore + "->" + getSelectedPosition()
                    + " focus " + (before == null ? "null" : before.getClass().getSimpleName() + " attached=" + before.isAttachedToWindow() + " shown=" + before.isShown())
                    + "->" + (after == null ? "null" : after.getClass().getSimpleName())
                    + " childCount=" + getChildCount() + " adapter=" + (getAdapter() == null ? -1 : getAdapter().getItemCount()));
        }
        return handled;
    }

    /**
     * 海报墙上下移动保持列对齐（第 30 轮：↑↓ 双向 + 按 x 坐标定向）。
     *
     * <p>Leanback 默认把焦点交给相邻行的「该行 selectedPosition」（未访问过/被回收重绑的行是 0），
     * 所以从第 3 列按 ↑/↓ 会跳到相邻行第 1 列（此前只修了 ↓，↑ 漏网——第 30 轮用户实机抓到）。
     * 两条路径：① 目标行已布局——焦点搜索阶段直接返回 x 最近列的子项；② 目标行在屏外未布局——
     * 记下 pending 列 x 交回 Leanback 滚动落焦，焦点落地瞬间由全局焦点监听器修正到最近列
     * （同一 vsync 内完成，无可见跳动）。列宽不一致的行（16:9 历史卡 vs 竖版海报）按 x 最近而非下标对齐。
     */
    private int mPendingColX = -1;
    private boolean mCorrectingFocus;

    @Override
    public View focusSearch(View focused, int direction) {
        if (direction == View.FOCUS_UP || direction == View.FOCUS_DOWN) {
            View sameColumn = findNearestColumn(focused, direction == View.FOCUS_DOWN ? 1 : -1);
            if (sameColumn != null) return sameColumn;
            markPendingColumn(focused, direction == View.FOCUS_DOWN ? 1 : -1);
        }
        return super.focusSearch(focused, direction);
    }

    /** 目标行未布局时记下当前列 x，等 Leanback 滚动落焦后由 onFocus 修正。 */
    private void markPendingColumn(View focused, int rowDelta) {
        mPendingColX = -1;
        if (focused == null || !(focused.getParent() instanceof HorizontalGridView)) return;
        View rowView = (View) focused.getParent().getParent();
        if (rowView == null) return;
        int rowPos = getChildAdapterPosition(rowView);
        if (rowPos < 0 || getAdapter() == null) return;
        int target = rowPos + rowDelta;
        if (target < 0 || target >= getAdapter().getItemCount()) return;
        android.graphics.Rect fr = new android.graphics.Rect();
        if (focused.getGlobalVisibleRect(fr)) mPendingColX = fr.centerX();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnGlobalFocusChangeListener(mFocusListener);
    }

    @Override
    protected void onDetachedFromWindow() {
        getViewTreeObserver().removeOnGlobalFocusChangeListener(mFocusListener);
        super.onDetachedFromWindow();
    }

    private final android.view.ViewTreeObserver.OnGlobalFocusChangeListener mFocusListener = (o, f) -> {
        if (mPendingColX < 0 || mCorrectingFocus || f == null) return;
        if (!(f.getParent() instanceof HorizontalGridView)) return;
        // 焦点刚落到目标行（Leanback 滚完后通常落在行首）：立刻修正到 pending 列 x 的最近项。
        HorizontalGridView row = (HorizontalGridView) f.getParent();
        View best = nearestChildByX(row, mPendingColX);
        if (best == null || best == f) {
            mPendingColX = -1;
            return;
        }
        mCorrectingFocus = true;
        mPendingColX = -1;
        post(() -> {
            best.requestFocus();
            mCorrectingFocus = false;
        });
    };

    private View findNearestColumn(View focused, int rowDelta) {
        if (focused == null) return null;
        View parent = (View) focused.getParent();
        if (!(parent instanceof HorizontalGridView)) return null;
        View rowView = (View) parent.getParent();
        if (rowView == null) return null;
        int rowPos = getChildAdapterPosition(rowView);
        if (rowPos < 0) return null;
        View targetRow = findChildByPosition(rowPos + rowDelta);
        if (targetRow == null) return null;
        HorizontalGridView next = targetRow.findViewById(androidx.leanback.R.id.row_content);
        if (next == null || next.getChildCount() == 0) return null;
        android.graphics.Rect fr = new android.graphics.Rect();
        if (!focused.getGlobalVisibleRect(fr)) return null;
        return nearestChildByX(next, fr.centerX());
    }

    /** 供 Activity 层跨区移动用：在第 rowPos 行里找与 focused 同列（x 最近）的子项；行未布局或无可聚焦项时返回 null。 */
    public View nearestColumnInRow(int rowPos, View focused) {
        if (focused == null) return null;
        android.graphics.Rect fr = new android.graphics.Rect();
        if (!focused.getGlobalVisibleRect(fr)) return null;
        View row = findChildByPosition(rowPos);
        if (row == null) return null;
        HorizontalGridView grid = row.findViewById(androidx.leanback.R.id.row_content);
        if (grid == null) return null;
        return nearestChildByX(grid, fr.centerX());
    }

    private View nearestChildByX(HorizontalGridView row, int fx) {
        android.graphics.Rect cr = new android.graphics.Rect();
        View best = null;
        int bestDx = Integer.MAX_VALUE;
        for (int i = 0; i < row.getChildCount(); i++) {
            View child = row.getChildAt(i);
            if (child == null || !child.isFocusable() || !child.getGlobalVisibleRect(cr)) continue;
            int dx = Math.abs(cr.centerX() - fx);
            if (dx < bestDx) {
                bestDx = dx;
                best = child;
            }
        }
        return best;
    }

    public boolean moveToTop() {
        if (views == null || getSelectedPosition() == 0 || getAdapter() == null || getAdapter().getItemCount() == 0) return false;
        for (View view : views) if (view.getId() == R.id.recycler) view.requestFocus();
        scrollToPosition(0);
        showHeader();
        return true;
    }
}
