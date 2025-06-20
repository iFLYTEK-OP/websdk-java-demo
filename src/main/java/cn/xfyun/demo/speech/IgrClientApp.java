package cn.xfyun.demo.speech;

import cn.xfyun.api.IgrClient;
import cn.xfyun.config.PropertiesConfig;
import cn.xfyun.model.response.igr.IgrResponseData;
import cn.xfyun.service.igr.AbstractIgrWebSocketListener;
import cn.xfyun.util.MicrophoneAudioSender;
import okhttp3.Response;
import okhttp3.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.security.SignatureException;
import java.util.Scanner;

/**
 * @author: <flhong2@iflytek.com>
 * @description: 性别年龄识别
 * @version: v1.0
 * @create: 2021-06-11 09:51
 **/
public class IgrClientApp {

    private static final Logger logger = LoggerFactory.getLogger(IatClientApp.class);
    private static final String appId = PropertiesConfig.getAppId();
    private static final String apiKey = PropertiesConfig.getApiKey();
    private static final String apiSecret = PropertiesConfig.getApiSecret();

    private static String filePath = "audio/cn/read_sentence_cn.pcm";
    private static String resourcePath;

    static {
        try {
            resourcePath = IgrClientApp.class.getResource("/").toURI().getPath();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws FileNotFoundException, SignatureException, MalformedURLException, InterruptedException {
        IgrClient igrClient = new IgrClient.Builder()
                .signature(appId, apiKey, apiSecret).ent("igr").aue("raw").rate(16000)
                .build();

        // 方式一：处理从文件中获取的音频数据
        processAudioFromFile(igrClient);

        // 方式二：处理麦克风输入的音频数据
        // processAudioFromMicrophone(igrClient);
    }

    /**
     * 处理麦克风输入的音频数据
     */
    public static void processAudioFromMicrophone(IgrClient igrClient) {
        // 麦克风工具类
        MicrophoneAudioSender sender = new MicrophoneAudioSender((audioData, length) -> {
            // 发送给 WebSocket
            igrClient.sendMessage(audioData, 1);
        });

        try (Scanner scanner = new Scanner(System.in)) {
            logger.info("按回车开始识别...");
            scanner.nextLine();

            igrClient.start(new AbstractIgrWebSocketListener() {
                @Override
                public void onSuccess(WebSocket webSocket, IgrResponseData igrResponseData) {
                    System.out.println("sid:" + igrResponseData.getSid());
                    System.out.println("识别结果为: " + igrResponseData.getData());
                    webSocket.close(1000, "");
                    System.exit(0);
                }

                @Override
                public void onFail(WebSocket webSocket, Throwable t, Response response) {

                }

                @Override
                public void onOpen(WebSocket webSocket, Response response) {
                    logger.info("连接成功");
                }
            });
            igrClient.sendMessage(null, 0);

            sender.start();

            logger.info("正在聆听，按回车结束听写...");
            scanner.nextLine();
            igrClient.sendMessage(null, 2);
        } catch (SignatureException e) {
            logger.error("API签名验证失败", e);
            throw new RuntimeException("服务鉴权异常，请检查密钥配置", e);
        } catch (IOException e) {
            logger.error("流操作异常", e);
            throw new RuntimeException("音频数据传输失败", e);
        } finally {
            sender.stop();
        }
    }

    public static void processAudioFromFile(IgrClient igrClient) throws FileNotFoundException, MalformedURLException, SignatureException {
        File file = new File(resourcePath + filePath);
        igrClient.send(file, new AbstractIgrWebSocketListener() {
            @Override
            public void onSuccess(WebSocket webSocket, IgrResponseData igrResponseData) {
                System.out.println("sid:" + igrResponseData.getSid());
                System.out.println("识别结果为: " + igrResponseData.getData());
                webSocket.close(1000, "");
                System.exit(0);
            }

            @Override
            public void onFail(WebSocket webSocket, Throwable t, Response response) {

            }
        });
    }
}
