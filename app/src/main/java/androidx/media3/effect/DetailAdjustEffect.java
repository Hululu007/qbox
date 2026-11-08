package androidx.media3.effect;

import androidx.media3.common.Effect;

/** QBox 兼容层：锐度/细节调节效果（占位实现，不改变画面）。 */
public final class DetailAdjustEffect implements Effect {

    private VideoAdjustment adjustment;

    public void setAdjustment(VideoAdjustment adjustment) {
        this.adjustment = adjustment;
    }

    public VideoAdjustment getAdjustment() {
        return adjustment;
    }
}
