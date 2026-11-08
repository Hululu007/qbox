package androidx.media3.common.audio;

/** QBox 兼容层：声道混合矩阵工具（简化实现，仅返回单位矩阵或简单映射）。 */
public final class AudioChannelMix {

    private AudioChannelMix() {
    }

    public static float[][] createFrontCenterGainMix(int channelCount, float gain) {
        return identity(channelCount);
    }

    public static float[][] createStereoMix(int channelCount, boolean reverse) {
        float[][] mix = identity(channelCount);
        if (reverse && channelCount >= 2) {
            float tmp = mix[0][0];
            mix[0][0] = mix[1][1];
            mix[1][1] = tmp;
        }
        return mix;
    }

    public static float[][] createMonoMix(int channelCount) {
        float[][] mix = new float[1][channelCount];
        for (int i = 0; i < channelCount; i++) mix[0][i] = 1.0f / Math.max(1, channelCount);
        return mix;
    }

    public static float[][] createFrontBalanceMix(int channelCount, float balance) {
        return identity(channelCount);
    }

    public static float[][] compose(float[][] first, float[][] second) {
        return first;
    }

    private static float[][] identity(int channelCount) {
        float[][] mix = new float[channelCount][channelCount];
        for (int i = 0; i < channelCount; i++) mix[i][i] = 1.0f;
        return mix;
    }
}
