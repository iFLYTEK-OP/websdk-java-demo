package cn.xfyun.util;

import javax.sound.sampled.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedOutputStream;

/**
 * 麦克风录音工具类
 * 支持16kHz采样率、16位深度、单声道PCM格式音频采集；
 * 基于后台线程提供非阻塞式的录音操作并输出符合语音服务要求的原始音频字节数据。
 */
public class MicrophoneRecorderUtil {
    
    /**
     * 音频格式配置参数（符合语音服务要求）
     * - 采样率：16000 Hz
     * - 采样位数：16 bit
     * - 单声道
     */
    private static final int SAMPLE_RATE = 16000;
    private static final int SAMPLE_SIZE_BITS = 16;
    private static final int CHANNELS = 1;
    
    private volatile TargetDataLine targetDataLine;
    
    private volatile boolean recording;
    private volatile PipedOutputStream outputStream;



    private ByteArrayOutputStream audioBuffer = new ByteArrayOutputStream();

    public void startRecording() throws LineUnavailableException {
        AudioFormat format = new AudioFormat(SAMPLE_RATE, SAMPLE_SIZE_BITS, CHANNELS, true, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        
        targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
        targetDataLine.open(format);
        targetDataLine.start();

        new Thread(() -> {
            byte[] buffer = new byte[1024];
            while (targetDataLine.isOpen()) {
                int bytesRead = targetDataLine.read(buffer, 0, buffer.length);
                if (bytesRead > 0) {
                    audioBuffer.write(buffer, 0, bytesRead);
                }
            }
        }).start();
    }

    public byte[] stopAndGetAudio() {
        if (targetDataLine != null) {
            targetDataLine.stop();
            targetDataLine.close();
        }
        return audioBuffer.toByteArray();
    }



    /**
     * 启动录音线程2
     * @throws LineUnavailableException 当无法获取音频输入设备时抛出此异常
     */
    public void startRecordingV2() throws LineUnavailableException {

        // 配置音频格式
        AudioFormat format = new AudioFormat(SAMPLE_RATE, SAMPLE_SIZE_BITS, CHANNELS, true, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        
        // 重置缓冲区并启动录音线程
        targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
        targetDataLine.open(format);
        targetDataLine.start();
        new Thread(() -> {
            try {
                captureAudio(); 
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

     // 录音线程逻辑
     private void captureAudio() {
        recording = true;
        byte[] buffer = new byte[1280]; 
        while (recording) {
            int bytesRead = targetDataLine.read(buffer, 0, buffer.length);
            if (bytesRead > 0 && outputStream!= null) {
                // 流式写入
                try {
                    outputStream.write(buffer, 0, bytesRead);
                    outputStream.flush();
                } catch (IOException e) {
                    System.err.println("流写入异常: " + e);
                    break;
                }
            }
        }
        
        // 录音停止时关闭流
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void stopRecording() {
        recording = false;
        // 音频设备关闭检查
        if (targetDataLine != null && targetDataLine.isOpen()) {
            targetDataLine.stop();
            targetDataLine.close();
        }
    }

    public void setOutputStream(PipedOutputStream outputStream) {
        this.outputStream = outputStream;
    }
}