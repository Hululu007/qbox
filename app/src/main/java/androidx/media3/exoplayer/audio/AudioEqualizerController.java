package androidx.media3.exoplayer.audio;

import android.content.Context;

import androidx.media3.common.audio.AudioEffectParameters;
import androidx.media3.exoplayer.ExoPlayer;

/** QBox 兼容层：系统均衡器控制器（简化实现，不真正挂接 AudioEffect）。 */
public final class AudioEqualizerController {

    public AudioEqualizerController(Context context) {
    }

    public boolean apply(ExoPlayer player, AudioEffectParameters parameters) {
        return true;
    }

    public void release() {
    }
}
