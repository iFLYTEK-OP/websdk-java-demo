package cn.xfyun.demo.speech;

import cn.xfyun.api.IseClient;
import cn.xfyun.config.PropertiesConfig;
import cn.xfyun.model.response.ise.IseResponseData;
import cn.xfyun.service.ise.AbstractIseWebSocketListener;
import cn.xfyun.util.MicrophoneAudioSender;
import cn.xfyun.util.MicrophoneRecorderUtil;
import okhttp3.Response;
import okhttp3.WebSocket;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.security.SignatureException;
import java.util.*;

import javax.sound.sampled.LineUnavailableException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ISE( iFly Speech Evaluator ) 语音评测
 * 1、APPID、APISecret、APIKey信息获取：<a href="https://console.xfyun.cn/services/ise">...</a>
 * 2、文档地址：<a href="https://www.xfyun.cn/doc/Ise/IseAPI.html">...</a>
 *
 * @author kaili23
 **/
public class IseClientApp {

    private static final Logger logger = LoggerFactory.getLogger(IseClientApp.class);

    /**
     * 服务鉴权参数
     */
    private static final String APP_ID = PropertiesConfig.getAppId();
    private static final String API_KEY = PropertiesConfig.getApiKey();
    private static final String API_SECRET = PropertiesConfig.getApiSecret();

    /**
     * 解码器
     */
    private static final Base64.Decoder DECODER = Base64.getDecoder();

    /**
     * 音频文件路径
     */
    private static String audioFilePath;

    /**
     * 语音评测客户端
     */
    private static final IseClient ISE_CLIENT;

    static {
        ISE_CLIENT = new IseClient.Builder()
                .signature(APP_ID, API_KEY, API_SECRET)
                .addSub("ise")
                .addEnt("cn_vip")
                .addCategory("read_sentence")
                .addTte("utf-8")
                // 待评测文本的utf8编码
                .addText('\uFEFF' + "今天天气怎么样")
                .addRstcd("utf8")
                .build();

        try {
            audioFilePath = Objects.requireNonNull(IseClientApp.class.getResource("/")).toURI().getPath() + "/audio/cn/read_sentence_cn.pcm";
        } catch (Exception e) {
            logger.error("资源路径获取失败", e);
        }
    }

    /**
     * WebSocket监听器实现，用于处理语音评测结果
     * 功能说明：
     * 1、成功回调：解码Base64格式的评测结果并输出；
     * 2、失败回调：记录通信异常状态。
     */
    private static final AbstractIseWebSocketListener ISE_LISTENER = new AbstractIseWebSocketListener() {

        @Override
        public void onSuccess(WebSocket webSocket, IseResponseData iseResponseData) {
            //解码Base64响应数据并转换为UTF-8字符串、中止JVM
            logger.info("sid：{}，最终评测结果：{}{}", iseResponseData.getSid(), System.lineSeparator(), new String(DECODER.decode(iseResponseData.getData().getData()), StandardCharsets.UTF_8));
            System.exit(0);
        }

        @Override
        public void onFail(WebSocket webSocket, Throwable throwable, Response response) {
            // 简单输出失败响应结果，实际生产环境建议添加重试逻辑
            logger.error("通信异常，服务端响应结果：{}", response, throwable);
            System.exit(0);
        }

    };

    public static void main(String[] args) throws SignatureException, InterruptedException, LineUnavailableException, IOException {
        // 方式一：处理从文件中获取的音频数据
        processAudioFromFile();

        // 方式二：处理麦克风输入的音频数据
        // processAudioFromMicrophone();

        // 方式三：用户自定义消息
        // processAudioFromCustom();
    }

    /**
     * 处理麦克风输入的音频数据
     */
    public static void processAudioFromCustom() {
        try (Scanner scanner = new Scanner(System.in)) {
            logger.info("本次评测内容为【{}】，按回车开始实时评测...", ISE_CLIENT.getText().replace("\uFEFF", ""));
            scanner.nextLine();

            // 麦克风工具类
            MicrophoneAudioSender sender = null;

            // 调用评测服务
            ISE_CLIENT.start(new AbstractIseWebSocketListener() {
                @Override
                public void onSuccess(WebSocket webSocket, IseResponseData iseResponseData) {
                    //解码Base64响应数据并转换为UTF-8字符串、中止JVM
                    logger.info("sid：{}，最终评测结果：{}{}", iseResponseData.getSid(), System.lineSeparator(), new String(DECODER.decode(iseResponseData.getData().getData()), StandardCharsets.UTF_8));
                    System.exit(0);
                }

                @Override
                public void onFail(WebSocket webSocket, Throwable throwable, Response response) {
                    // 简单输出失败响应结果，实际生产环境建议添加重试逻辑
                    logger.error("通信异常，服务端响应结果：{}", response, throwable);
                    System.exit(0);
                }

                @Override
                public void onOpen(WebSocket webSocket, Response response) {
                    logger.info("连接成功");

                }
            });
            ISE_CLIENT.sendMessage(null, 0);
            sender = new MicrophoneAudioSender((audioData, length) -> {
                // 发送给 WebSocket
                ISE_CLIENT.sendMessage(audioData, 1);
            });
            sender.start();

            logger.info("正在聆听，按回车结束评测...");
            scanner.nextLine();
            ISE_CLIENT.sendMessage(null, 2);
        } catch (SignatureException e) {
            logger.error("API签名验证失败", e);
            throw new RuntimeException("服务鉴权异常，请检查密钥配置", e);
        } catch (IOException e) {
            logger.error("流操作异常", e);
            throw new RuntimeException("音频数据传输失败", e);
        }
    }

    /**
     * 处理从文件中获取的音频数据
     */
    public static void processAudioFromFile() {
        try {
            File file = new File(audioFilePath);
            ISE_CLIENT.send(file, ISE_LISTENER);
        } catch (FileNotFoundException e) {
            logger.error("音频文件未找到，路径为：{}", audioFilePath, e);
            throw new RuntimeException("音频文件加载失败，请检查路径：" + audioFilePath);
        } catch (MalformedURLException e) {
            logger.error("无效的URL格式", e);
            throw new RuntimeException("音频服务地址配置错误", e);
        } catch (SignatureException e) {
            logger.error("API签名异常", e);
            throw new RuntimeException("服务鉴权失败，请检查API密钥配置");
        }
    }

    /**
     * 处理麦克风输入的音频数据
     */
    public static void processAudioFromMicrophone() {
        MicrophoneRecorderUtil recorder = null;

        try (Scanner scanner = new Scanner(System.in)) {
            logger.info("本次评测内容为【{}】，按回车开始实时评测...", ISE_CLIENT.getText().replace("\uFEFF", ""));
            scanner.nextLine();

            // 创建带缓冲的音频管道流
            PipedInputStream audioInputStream = new PipedInputStream();
            PipedOutputStream audioOutputStream = new PipedOutputStream(audioInputStream);

            // 配置录音工具
            recorder = new MicrophoneRecorderUtil();

            // 开始录音并初始化状态
            recorder.startRecording(audioOutputStream);

            // 调用评测服务
            ISE_CLIENT.send(audioInputStream, ISE_LISTENER);

            logger.info("正在聆听，按回车结束评测...");
            scanner.nextLine();
        } catch (LineUnavailableException e) {
            logger.error("录音设备不可用", e);
            throw new RuntimeException("麦克风初始化失败，请检查录音设备", e);
        } catch (SignatureException e) {
            logger.error("API签名验证失败", e);
            throw new RuntimeException("服务鉴权异常，请检查密钥配置", e);
        } catch (IOException e) {
            logger.error("流操作异常", e);
            throw new RuntimeException("音频数据传输失败", e);
        } finally {
            // 释放资源
            if (recorder != null) {
                recorder.stopRecording();
            }
            // 此处取消了手动关闭WebSocket连接，listener收到服务端响应后再关闭连接。
        }
    }

}
