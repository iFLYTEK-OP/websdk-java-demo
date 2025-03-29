package cn.xfyun.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 音频调起麦克风播放工具类
 * 支持16kHz采样率、16位深度、单声道PCM格式音频播放；
 *
 * @author zyding6
 */
public class AudioPlayer {
    private static final Logger logger = LoggerFactory.getLogger(AudioPlayer.class);
    private final BlockingQueue<byte[]> audioQueue = new LinkedBlockingQueue<>();
    private final AtomicBoolean isPlaying = new AtomicBoolean(false);
    private Thread playerThread;
    private SourceDataLine line;

    // 音频参数
    private final float sampleRate;
    private final int sampleSizeInBits;
    private final int channels;
    private final boolean signed;
    private final boolean bigEndian;

    public AudioPlayer() {
        // 默认 16kHz, 16bit, 单声道, PCM
        this(16000, 16, 1, true, false);
    }

    public AudioPlayer(float sampleRate, int sampleSizeInBits, int channels, boolean signed, boolean bigEndian) {
        this.sampleRate = sampleRate;
        this.sampleSizeInBits = sampleSizeInBits;
        this.channels = channels;
        this.signed = signed;
        this.bigEndian = bigEndian;
    }

    /**
     * 启动播放线程
     */
    public void start() {
        if (isPlaying.get()) return;
        isPlaying.set(true);

        playerThread = new Thread(() -> {
            try {
                AudioFormat format = new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(format);
                line.start();

                final int frameSize = format.getFrameSize();

                while (isPlaying.get() || !audioQueue.isEmpty()) {
                    byte[] audioData = audioQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (audioData != null) {
                        safeWrite(line, audioData, frameSize);
                    }
                    TimeUnit.MILLISECONDS.sleep(20);
                }

                line.drain();
                line.stop();
                line.close();
                logger.info("播放器线程正常结束");

            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        playerThread.setName("Audio-Player-Thread");
        playerThread.start();
    }

    /**
     * 添加音频数据到播放队列
     *
     * @param audioData PCM字节数组
     */
    public void play(byte[] audioData) {
        if (!isPlaying.get()) {
            throw new IllegalStateException("AudioPlayer未启动");
        }
        try {
            audioQueue.put(audioData); // 队列写入
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * 关闭播放器，保证剩余音频播放完再退出
     */
    public void stop() {
        isPlaying.set(false);
        try {
            if (playerThread != null) {
                playerThread.join();
            }
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * 确保写入数据完整 frame 对齐
     */
    private void safeWrite(SourceDataLine line, byte[] audioData, int frameSize) {
        int length = audioData.length;
        int remainder = length % frameSize;
        if (remainder != 0) {
            length -= remainder; // 防止非整帧
        }
        line.write(audioData, 0, length);
    }
}
