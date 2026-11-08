package androidx.media3.common.audio;

/** QBox 兼容层：原版 FongMi media3 fork 的声道模式常量与工具方法（简化实现）。 */
public final class AudioChannelMode {

    public static final int AUTO = -1;
    public static final int STEREO = 0;
    public static final int MONO = 1;
    public static final int REVERSE = 2;

    private AudioChannelMode() {
    }

    public static int clamp(int mode) {
        return mode >= MONO && mode <= REVERSE ? mode : AUTO;
    }

    public static boolean canBalance(int mode) {
        return mode != MONO;
    }

    public static boolean isAuto(int mode) {
        return mode == AUTO;
    }

    public static boolean isAvailable(int channelCount) {
        return channelCount >= 2;
    }

    public static int resolve(int mode, int channelCount) {
        if (channelCount < 2) return MONO;
        return mode == MONO || mode == REVERSE ? mode : STEREO;
    }

    public static boolean isAvailable(int channelMode, int channelCount) {
        return channelCount >= 2;
    }
}
