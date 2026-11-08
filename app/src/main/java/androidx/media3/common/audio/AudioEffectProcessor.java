package androidx.media3.common.audio;

import java.nio.ByteBuffer;

/** QBox 兼容层：软件音效处理器（透传实现）。 */
public class AudioEffectProcessor extends BaseAudioProcessor {

    private AudioEffectParameters parameters;

    public void setParameters(AudioEffectParameters parameters, boolean softwareEqualizer) {
        this.parameters = parameters;
    }

    public void resetParameters() {
        parameters = null;
    }

    public AudioEffectParameters getParameters() {
        return parameters;
    }

    @Override
    public void queueInput(ByteBuffer inputBuffer) {
        if (!inputBuffer.hasRemaining()) return;
        ByteBuffer output = replaceOutputBuffer(inputBuffer.remaining());
        output.put(inputBuffer);
        output.flip();
    }
}
