package androidx.media3.effect;

/** QBox 兼容层：视频调节参数（色相/饱和/对比等），对应 FongMi media3 fork。 */
public final class VideoAdjustment {

    public final float saturation;
    public final float contrast;
    public final float brightness;
    public final float gamma;
    public final float hue;
    public final float redGain;
    public final float blueGain;
    public final float sharpness;
    public final float threshold;
    public final float shadowLift;

    private VideoAdjustment(Builder builder) {
        this.saturation = builder.saturation;
        this.contrast = builder.contrast;
        this.brightness = builder.brightness;
        this.gamma = builder.gamma;
        this.hue = builder.hue;
        this.redGain = builder.redGain;
        this.blueGain = builder.blueGain;
        this.sharpness = builder.sharpness;
        this.threshold = builder.threshold;
        this.shadowLift = builder.shadowLift;
    }

    public static final class Builder {

        private float saturation = 1.0f;
        private float contrast = 1.0f;
        private float brightness = 0.0f;
        private float gamma = 1.0f;
        private float hue = 0.0f;
        private float redGain = 1.0f;
        private float blueGain = 1.0f;
        private float sharpness = 0.0f;
        private float threshold = 0.0f;
        private float shadowLift = 0.0f;

        public Builder setSaturation(float saturation) {
            this.saturation = saturation;
            return this;
        }

        public Builder setContrast(float contrast) {
            this.contrast = contrast;
            return this;
        }

        public Builder setBrightness(float brightness) {
            this.brightness = brightness;
            return this;
        }

        public Builder setGamma(float gamma) {
            this.gamma = gamma;
            return this;
        }

        public Builder setHue(float hue) {
            this.hue = hue;
            return this;
        }

        public Builder setRedGain(float redGain) {
            this.redGain = redGain;
            return this;
        }

        public Builder setBlueGain(float blueGain) {
            this.blueGain = blueGain;
            return this;
        }

        public Builder setSharpness(float sharpness) {
            this.sharpness = sharpness;
            return this;
        }

        public Builder setThreshold(float threshold) {
            this.threshold = threshold;
            return this;
        }

        public Builder setShadowLift(float shadowLift) {
            this.shadowLift = shadowLift;
            return this;
        }

        public VideoAdjustment build() {
            return new VideoAdjustment(this);
        }
    }
}
