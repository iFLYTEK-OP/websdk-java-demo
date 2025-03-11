package cn.xfyun.demo;

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


/**
 * IAT( iFly Auto Transform ) 语音听写
 * 1、APPID、APISecret、APIKey信息获取：https://console.xfyun.cn/services/iat
 * 2、文档地址：https://www.xfyun.cn/doc/asr/voicedictation/API.html
 */
public class IatClientAppV2 {

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
                .dwa("wpgs")
                .build();
                
        try {
            resourcePath = IatClientAppV2.class.getResource("/").toURI().getPath();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * WebSocket监听器实现，用于处理语音听写结果
     * 功能说明：
     * 1、成功回调：解析中间/最终结果，处理错误码；
     * 2、失败回调：自定义处理（记录通信异常等）。
     */
    private static final AbstractIatWebSocketListener iatWebSocketListener = new AbstractIatWebSocketListener() {

        @Override
        public void onSuccess(WebSocket webSocket, IatResponse iatResponse) {

            if (iatResponse.getCode() != 0) {
                System.out.println("code=>" + iatResponse.getCode() + " error=>" + iatResponse.getMessage() + " sid=" + iatResponse.getSid());
                System.out.println("错误码查询链接：https://www.xfyun.cn/document/error-code");
                return;
            }

            if (iatResponse.getData() != null) {
                if (iatResponse.getData().getResult() != null) {
                    IatResult result = iatResponse.getData().getResult();
                    Text textObject = result.getText();

                    // 处理返回结果
                    handleResultText(textObject);
                    System.out.println("中间识别结果 ==》" + textObject.getText());
                }

                if (iatResponse.getData().getStatus() == 2) {
                    // resp.data.status ==2 说明数据全部返回完毕，可以关闭连接，释放资源
                    System.out.println("session end ");
                    Date dateEnd = new Date();
                    System.out.println(sdf.format(dateBegin) + "开始");
                    System.out.println(sdf.format(dateEnd) + "结束");
                    System.out.println("耗时:" + (dateEnd.getTime() - dateBegin.getTime()) + "ms");
                    System.out.println("最终识别结果 ==》" + getFinalResult());
                    System.out.println("本次识别sid ==》" + iatResponse.getSid());
                    iatClient.closeWebsocket();
                    System.exit(0);
                } else {
                    // 根据返回的数据处理
                    // System.out.println(StringUtils.gson.toJson(iatResponse));
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
        // processAudioFromFile();

        // 方式二：处理麦克风输入的音频数据
        processAudioFromMicrophone();
    }

    /**
     * 处理从文件中获取的音频数据
     */
    public static void processAudioFromFile() throws LineUnavailableException, MalformedURLException, SignatureException, FileNotFoundException {
        
        File file = new File(resourcePath + filePath);

        // 记录操作耗时与最终结果
        dateBegin = new Date();
        resultSegments = new ArrayList<>();
        
        // 发送音频数据
        iatClient.send(file, iatWebSocketListener);
    }

    /**
     * 处理麦克风输入的音频数据
     */
    public static void processAudioFromMicrophone() throws LineUnavailableException, SignatureException, IOException {

        Scanner scanner = new Scanner(System.in);
        System.out.println("按回车开始实时听写...");
        scanner.nextLine();

        // 创建带缓冲的音频管道流
        PipedInputStream audioInputStream = new PipedInputStream();
        PipedOutputStream audioOutputStream = new PipedOutputStream(audioInputStream);
        
        // 配置录音工具
        MicrophoneRecorderUtil recorder = new MicrophoneRecorderUtil();
        recorder.setOutputStream(audioOutputStream);
        
        // 开始录音并初始化状态
        recorder.startRecordingV2();
        dateBegin = new Date();
        resultSegments = new ArrayList<>();

        // 启动流式听写
        iatClient.send(audioInputStream, iatWebSocketListener);

        System.out.println("正在聆听，按回车结束听写...");
        scanner.nextLine();
        
        // 停止录音并关闭资源
        recorder.stopRecording();
        iatClient.closeWebsocket();
        scanner.close();
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
            // System.out.println("替换操作，服务端返回结果为：" + textObject);
        }
        // 通用逻辑，添加当前文本到结果列表
        resultSegments.add(textObject);
    }

    /**
     * 生成最终结果
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