package cn.xfyun.demo;

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
import java.net.URISyntaxException;
import java.security.SignatureException;
import java.util.Base64;
import java.util.Scanner;

import javax.sound.sampled.LineUnavailableException;

/**
 * ISE( iFly Speech Evaluator ) 语音评测
 * 1、APPID、APISecret、APIKey信息获取：https://console.xfyun.cn/services/ise
 * 2、文档地址：https://www.xfyun.cn/doc/Ise/IseAPI.html
 **/
public class IseClientAppV2 {

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
            resourcePath = IseClientAppV2.class.getResource("/").toURI().getPath();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    /**
     * WebSocket监听器实现，用于处理语音评测结果
     * 功能说明：
     * 1、成功回调：解码Base64格式的评测结果并输出；
     * 2、失败回调：记录通信异常状态。
     */
    private static final AbstractIseWebSocketListener iseWebSocketListener = new AbstractIseWebSocketListener() {
    
        @Override
        public void onSuccess(WebSocket webSocket, IseResponseData iseResponseData) {
            try {
                //解码Base64响应数据并转换为UTF-8字符串、中止JVM
                System.out.println("sid:" + iseResponseData.getSid() + "\n最终识别结果:\n" + 
                    new String(decoder.decode(iseResponseData.getData().getData()), "UTF-8"));
                System.exit(0); 
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    
        @Override
        public void onFail(WebSocket webSocket, Throwable throwable, Response response) {
            // 简单输出失败响应结果，实际生产环境建议添加重试逻辑
            System.out.println(response);
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
    public static void processAudioFromFile() throws LineUnavailableException, MalformedURLException, SignatureException, FileNotFoundException {
        
        File file = new File(resourcePath + filePath);
        
        // 发送音频数据
        iseClient.send(file, iseWebSocketListener);
    }

    /**
     * 处理麦克风输入的音频数据
     * @throws IOException 
     */
    public static void processAudioFromMicrophone() throws LineUnavailableException, SignatureException, IOException {
        
        Scanner scanner = new Scanner(System.in);
        System.out.println("本次评测内容为：【" + iseClient.getText().replace("\uFEFF", "") + "】。按回车开始实时评测...");
        scanner.nextLine();

        // 创建带缓冲的音频管道流
        PipedInputStream audioInputStream = new PipedInputStream();
        PipedOutputStream audioOutputStream = new PipedOutputStream(audioInputStream);
        
        // 配置录音工具
        MicrophoneRecorderUtil recorder = new MicrophoneRecorderUtil();
        recorder.setOutputStream(audioOutputStream);
        
        // 开始录音并初始化状态
        recorder.startRecordingV2();

        // 启动流式评测
        iseClient.send(audioInputStream, iseWebSocketListener);

        System.out.println("正在聆听，按回车结束评测...");
        scanner.nextLine();
        
        // 停止录音并关闭资源
        recorder.stopRecording();
        iseClient.closeWebsocket();
        scanner.close();
    }
}
