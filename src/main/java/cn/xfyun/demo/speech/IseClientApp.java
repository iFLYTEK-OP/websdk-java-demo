package cn.xfyun.demo.speech;

import cn.xfyun.api.IseClient;
import cn.xfyun.config.PropertiesConfig;
import cn.xfyun.model.response.ise.IseResponseData;
import cn.xfyun.service.ise.AbstractIseWebSocketListener;
import cn.xfyun.util.MicrophoneRecorderUtil;
import okhttp3.Response;
import okhttp3.WebSocket;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.security.SignatureException;
import java.util.Base64;
import java.util.Scanner;

import javax.sound.sampled.LineUnavailableException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ISE( iFly Speech Evaluator ) 语音评测
 * 1、APPID、APISecret、APIKey信息获取：https://console.xfyun.cn/services/ise
 * 2、文档地址：https://www.xfyun.cn/doc/Ise/IseAPI.html
 **/
public class IseClientApp {

    private static final Logger logger = LoggerFactory.getLogger(IseClientApp.class);

    /**
     * 服务鉴权参数
     */
    private static final String appId = PropertiesConfig.getAppId();
    private static final String apiKey = PropertiesConfig.getApiKey();
    private static final String apiSecret = PropertiesConfig.getApiSecret();

    /**
     * 解码器
     */
    private static final Base64.Decoder decoder = Base64.getDecoder();

    /**
     * 音频文件路径
     */
    private static String filePath = "audio/cn/read_sentence_cn.pcm";
    private static String resourcePath;

    /**
     * 语音评测客户端
     */
    private static IseClient iseClient;

    /**
     * 静态变量初始化
     */
    static {
        iseClient = new IseClient.Builder()
                .signature(appId, apiKey, apiSecret)
                .addSub("ise")
                .addEnt("cn_vip")
                .addCategory("read_sentence")
                .addTte("utf-8")
                // 待评测文本的utf8编码
                .addText('\uFEFF' + "今天天气怎么样")
                .addRstcd("utf8")
                .build();

        try {
            resourcePath = IseClientApp.class.getResource("/").toURI().getPath();
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
    private static final AbstractIseWebSocketListener iseListener = new AbstractIseWebSocketListener() {
    
        @Override
        public void onSuccess(WebSocket webSocket, IseResponseData iseResponseData) {
            try {
                //解码Base64响应数据并转换为UTF-8字符串、中止JVM
                logger.info("sid：{}，最终评测结果：{}{}", iseResponseData.getSid(), System.lineSeparator(), new String(decoder.decode(iseResponseData.getData().getData()), "UTF-8"));
                System.exit(0); 
            } catch (UnsupportedEncodingException e) {
                logger.error("解码失败", e);
            }
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
        // processAudioFromFile();

        // 方式二：处理麦克风输入的音频数据
        processAudioFromMicrophone();
    }

    /**
     * 处理从文件中获取的音频数据
     */
    public static void processAudioFromFile() {
        String completeFilePath = resourcePath + filePath;

        try {
            File file = new File(completeFilePath);
            // 发送音频数据
            iseClient.send(file, iseListener);
        } catch (FileNotFoundException e) {
            logger.error("音频文件未找到，路径为：{}", completeFilePath, e);
            throw new RuntimeException("音频文件加载失败，请检查路径：" + completeFilePath);
        } catch (MalformedURLException e) {
            logger.error("无效的URL格式，异常信息：{}", e);
            throw new RuntimeException("音频服务地址配置错误", e);
        } catch (SignatureException e) {
            logger.error("API签名异常", e);
            throw new RuntimeException("服务鉴权失败，请检查API密钥配置");
        }
    }

    /**
     * 处理麦克风输入的音频数据
     * @throws IOException 
     */
    public static void processAudioFromMicrophone() {
        Scanner scanner = null;
        MicrophoneRecorderUtil recorder = null;

        try {
            scanner = new Scanner(System.in);
            logger.info("本次评测内容为【{}】，按回车开始实时评测...", iseClient.getText().replace("\uFEFF", ""));
            scanner.nextLine();
    
            // 创建带缓冲的音频管道流
            PipedInputStream audioInputStream = new PipedInputStream();
            PipedOutputStream audioOutputStream = new PipedOutputStream(audioInputStream);
            
            // 配置录音工具
            recorder = new MicrophoneRecorderUtil();

            // 开始录音并初始化状态
            recorder.startRecording(audioOutputStream);

            // 调用评测服务
            iseClient.send(audioInputStream, iseListener);

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
            if (scanner != null) {
                scanner.close();
            }
            if (recorder != null) {
                recorder.stopRecording();
            }
            // 此处取消了手动关闭WebSocket连接，listener收到服务端响应后再关闭连接。
        }
    }

}
