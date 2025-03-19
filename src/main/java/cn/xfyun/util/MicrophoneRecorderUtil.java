package cn.xfyun.util;

import javax.sound.sampled.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PipedOutputStream;

/**
 * 麦克风录音工具类
 * 支持16kHz采样率、16位深度、单声道PCM格式音频采集；
 * 基于后台线程提供非阻塞式的录音操作并输出符合语音服务要求的原始音频字节数据。
 *
 * @author kaili23
 */
public class MicrophoneRecorderUtil {

    private static final Logger logger = LoggerFactory.getLogger(MicrophoneRecorderUtil.class);

    /**
     * 音频格式配置参数（符合语音服务要求）
     * - 采样率：16000 Hz
     * - 采样位数：16 bit
     * - 声道：单声道
     * - 采样数据：带符号
     * - 字节存储顺序：小端模式
     */
    private static final int SAMPLE_RATE = 16000;
    private static final int SAMPLE_SIZE_BITS = 16;
    private static final int CHANNELS = 1;
    private static final boolean SIGNED = true;
    private static final boolean BIG_ENDIAN = false;

    /**
     * 录音设备句柄、状态标志与输出流
     * volatile保证多线程下的可见性
     */
    private volatile TargetDataLine targetDataLine;
    private volatile boolean recording;
    private volatile PipedOutputStream outputStream;

    /**
     * 启动录音任务
     *
     * @throws LineUnavailableException 当音频设备不可用时抛出
     * @throws IllegalArgumentException 当输出流为null时抛出
     * @throws IllegalStateException    当已有录音任务运行时抛出
     */
    public synchronized void startRecording(PipedOutputStream outputStream) throws LineUnavailableException {
        if (outputStream == null) {
            throw new IllegalArgumentException("输出流不能为空");
        }

        if (recording || this.outputStream != null) {
            throw new IllegalStateException("已有录音任务在执行中");
        }

        this.outputStream = outputStream;

        // 配置音频格式
        AudioFormat format = new AudioFormat(SAMPLE_RATE, SAMPLE_SIZE_BITS, CHANNELS, SIGNED, BIG_ENDIAN);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        // 获取系统录音设备数据线路，应用音频格式配置，开始采集音频数据
        try {
            targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
            targetDataLine.open(format);
            targetDataLine.start();
        } catch (LineUnavailableException e) {
            cleanupResources();
            throw e;
        }

        // 启动独立录音线程
        Thread captureThread = new Thread(this::captureAudio);
        captureThread.setUncaughtExceptionHandler((thread, throwable) -> {
            logger.error("录音线程异常", throwable);
            cleanupResources();
        });
        captureThread.start();
    }

    /**
     * 采集音频数据
     * 数据流：麦克风 -> TargetDataLine -> buffer数组 -> PipedOutputStream -> PipedInputStream（由调用方持有）。
     */
    private void captureAudio() {
        recording = true;
        byte[] buffer = new byte[1280];

        // 循环读取音频数据
        while (recording) {
            int bytesRead = targetDataLine.read(buffer, 0, buffer.length);
            if (bytesRead > 0 && outputStream != null) {
                // 流式写入
                try {
                    outputStream.write(buffer, 0, bytesRead);
                    outputStream.flush();
                } catch (IOException e) {
                    logger.error("输出流写入异常", e);
                    break;
                }
            }
        }

        cleanupResources();
    }

    /**
     * 停止录音任务
     */
    public synchronized void stopRecording() {
        recording = false;
    }

    /**
     * 统一资源清理方法
     */
    private synchronized void cleanupResources() {
        try {
            // 关闭音频设备
            if (targetDataLine != null && targetDataLine.isOpen()) {
                targetDataLine.stop();
                targetDataLine.close();
            }
            // 关闭输出流
            if (outputStream != null) {
                outputStream.close();
            }
        } catch (Exception e) {
            logger.error("资源清理异常", e);
        } finally {
            targetDataLine = null;
            outputStream = null;
        }
    }

}