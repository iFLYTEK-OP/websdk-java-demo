package cn.xfyun.demo.speech;

import cn.xfyun.api.IatClient;
import cn.xfyun.config.PropertiesConfig;
import cn.xfyun.model.response.iat.IatResponse;
import cn.xfyun.model.response.iat.IatResult;
import cn.xfyun.model.response.iat.Text;
import cn.xfyun.service.iat.AbstractIatWebSocketListener;
import cn.xfyun.util.MicrophoneRecorderUtil;
import okhttp3.Response;
import okhttp3.WebSocket;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.MalformedURLException;
import java.security.SignatureException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import javax.sound.sampled.LineUnavailableException;

import org.apache.commons.codec.binary.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * IAT( iFly Auto Transform ) 语音听写
 * 1、APPID、APISecret、APIKey信息获取：https://console.xfyun.cn/services/iat
 * 2、文档地址：https://www.xfyun.cn/doc/asr/voicedictation/API.html
 */
public class IatClientApp {

    private static final Logger logger = LoggerFactory.getLogger(IatClientApp.class);

    /**
     * 服务鉴权参数
     */
    private static final String appId = PropertiesConfig.getAppId();
    private static final String apiKey = PropertiesConfig.getApiKey();
    private static final String apiSecret = PropertiesConfig.getApiSecret();

    /**
     * 音频文件路径
     */
    private static String filePath = "audio/iat_pcm_16k.pcm";
    private static String resourcePath;

    /**
     * 记录操作耗时与完整结果
     */
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyy-MM-dd HH:mm:ss.SSS");
    private static Date dateBegin;
    private static List<Text> resultSegments; 

    /**
     * 语音听写客户端
     */
    private static IatClient iatClient;

    /**
     * 静态变量初始化
     */
    static {
        iatClient = new IatClient.Builder()
                .signature(appId, apiKey, apiSecret)
                // 动态修正功能：值为wpgs时代表开启（包含修正功能的）流式听写
                .dwa("wpgs")
                .build();
                
        try {
            resourcePath = IatClientApp.class.getResource("/").toURI().getPath();
        } catch (Exception e) {
            logger.error("资源路径获取失败", e);
        }
    }

    /**
     * WebSocket监听器实现，用于处理语音听写结果
     * 功能说明：
     * 1、成功回调：解析中间/最终结果，处理错误码；
     * 2、失败回调：自定义处理（记录通信异常等）。
     */
    private static final AbstractIatWebSocketListener iatListener = new AbstractIatWebSocketListener() {
       
        @Override
        public void onSuccess(WebSocket webSocket, IatResponse iatResponse) {
            if (iatResponse.getCode() != 0) {
                logger.warn("code：{}, error：{}, sid：{}", iatResponse.getCode(), iatResponse.getMessage(), iatResponse.getSid());
                logger.warn("错误码查询链接：https://www.xfyun.cn/document/error-code");
                return;
            }

            if (iatResponse.getData() != null) {
                if (iatResponse.getData().getResult() != null) {
                    // 解析服务端返回结果
                    IatResult result = iatResponse.getData().getResult();
                    Text textObject = result.getText();
                    handleResultText(textObject);
                    logger.info("中间识别结果：{}", getFinalResult());
                }

                if (iatResponse.getData().getStatus() == 2) {
                    // resp.data.status ==2 说明数据全部返回完毕，可以关闭连接，释放资源
                    logger.info("session end ");
                    Date dateEnd = new Date();
                    logger.info("识别开始时间：{}，识别结束时间：{}，总耗时：{}ms", sdf.format(dateBegin), sdf.format(dateEnd), dateEnd.getTime() - dateBegin.getTime());
                    logger.info("最终识别结果：【{}】，本次识别sid：{}", getFinalResult(), iatResponse.getSid());
                    iatClient.closeWebsocket();
                    System.exit(0);
                } else {
                    // 根据返回的数据自定义处理逻辑
                }
            }
        }

        @Override
        public void onFail(WebSocket webSocket, Throwable t, Response response) {
            // 自定义处理逻辑
        }

    };

    public static void main(String[] args) throws SignatureException, LineUnavailableException, IOException {
        // 方式一：处理从文件中获取的音频数据
        processAudioFromFile();

        // 方式二：处理麦克风输入的音频数据
        // processAudioFromMicrophone();
    }

    /**
     * 处理从文件中获取的音频数据
     */
    public static void processAudioFromFile() {
        // 记录操作耗时与最终结果
        dateBegin = new Date();
        resultSegments = new ArrayList<>();
        String completeFilePath = resourcePath + filePath;

        try {
            File file = new File(completeFilePath);
            // 发送音频数据
            iatClient.send(file, iatListener);
        } catch (FileNotFoundException e) {
            logger.error("音频文件未找到，路径：{}", completeFilePath, e);
            throw new RuntimeException("音频文件加载失败，请检查路径：" + completeFilePath);
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
        Scanner scanner = null;
        MicrophoneRecorderUtil recorder = null;

        try {
            scanner = new Scanner(System.in);
            logger.info("按回车开始实时听写...");
            scanner.nextLine();
    
            // 创建带缓冲的音频管道流
            PipedInputStream audioInputStream = new PipedInputStream();
            PipedOutputStream audioOutputStream = new PipedOutputStream(audioInputStream);
            
            // 配置录音工具
            recorder = new MicrophoneRecorderUtil();

            // 开始录音并初始化状态
            dateBegin = new Date();
            resultSegments = new ArrayList<>();
            recorder.startRecording(audioOutputStream);

            // 调用流式听写服务
            iatClient.send(audioInputStream, iatListener);

            logger.info("正在聆听，按回车结束听写...");
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
            // 此处取消了手动关闭WebSocket连接，listener收到服务端最后一帧后再关闭连接。
        }
    }

    /**
     * 处理返回结果（包括全量返回与流式返回（结果修正））
     */
    private static void handleResultText(Text textObject) {
        // 处理流式返回的替换结果
        if (StringUtils.equals(textObject.getPgs(), "rpl") && textObject.getRg()!= null && textObject.getRg().length == 2) {
            // 返回结果序号sn字段的最小值为1
            int start = textObject.getRg()[0] - 1;
            int end = textObject.getRg()[1] - 1;
            
            // 将指定区间的结果设置为删除状态
            for (int i = start; i <= end && i < resultSegments.size(); i++) {
                resultSegments.get(i).setDeleted(true);
            }
            // logger.info("替换操作，服务端返回结果为：" + textObject);
        }

        // 通用逻辑，添加当前文本到结果列表
        resultSegments.add(textObject);
    }

    /**
     * 获取最终结果
     */
    private static String getFinalResult() {
        StringBuilder finalResult = new StringBuilder();
        for (Text text : resultSegments) {
            if (text != null && !text.isDeleted()) {
                finalResult.append(text.getText());
            }
        }
        return finalResult.toString();
    }

}