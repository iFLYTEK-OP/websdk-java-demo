package cn.xfyun.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @program: websdk-java
 * @description:
 * @author: zyding6
 * @create: 2025/3/24 9:58
 **/
public class MicrophoneAudioSender {

    private static final Logger logger = LoggerFactory.getLogger(MicrophoneAudioSender.class);

    public interface AudioDataCallback {
        void onAudioData(byte[] audioData, int length);
    }

    private final float sampleRate;
    private final int sampleSizeInBits;
    private final int channels;
    private final boolean signed;
    private final boolean bigEndian;
    private final int bufferSize;
    private final AudioDataCallback callback;

    private TargetDataLine microphoneLine;
    private AtomicBoolean isRunning = new AtomicBoolean(false);
    private Thread captureThread;

    public MicrophoneAudioSender(AudioDataCallback callback) {
        // 默认参数 16kHz 16bit 单声道
        this(16000, 16, 1, true, false, 4096, callback);
    }

    public MicrophoneAudioSender(float sampleRate, int sampleSizeInBits, int channels, boolean signed, boolean bigEndian,
                                 int bufferSize, AudioDataCallback callback) {
        this.sampleRate = sampleRate;
        this.sampleSizeInBits = sampleSizeInBits;
        this.channels = channels;
        this.signed = signed;
        this.bigEndian = bigEndian;
        this.bufferSize = bufferSize;
        this.callback = callback;
    }

    /**
     * 启动麦克风采集
     */
    public void start() {
        if (isRunning.get()) return;
        isRunning.set(true);

        captureThread = new Thread(() -> {
            try {
                AudioFormat format = new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                microphoneLine = (TargetDataLine) AudioSystem.getLine(info);
                microphoneLine.open(format);
                microphoneLine.start();

                byte[] buffer = new byte[bufferSize];

                while (isRunning.get()) {
                    int count = microphoneLine.read(buffer, 0, buffer.length);
                    if (count > 0) {
                        callback.onAudioData(buffer, count); // 回调实时数据
                    }
                    // TimeUnit.MILLISECONDS.sleep(20);
                }

                microphoneLine.stop();
                microphoneLine.close();
                logger.info("麦克风采集线程结束");

            } catch (Exception e) {
                logger.error("麦克风采集出错", e);
            }
        });

        captureThread.setName("Microphone-Capture-Thread");
        captureThread.start();
    }

    /**
     * 停止采集
     */
    public void stop() {
        isRunning.set(false);
        try {
            if (captureThread != null) {
                captureThread.join();
            }
        } catch (InterruptedException e) {
            logger.error("麦克风关闭出错", e);
        }
    }
}
