package cn.xfyun.demo.spark;

import cn.xfyun.api.OralClient;
import cn.xfyun.config.PropertiesConfig;
import cn.xfyun.model.oral.response.OralResponse;
import cn.xfyun.service.oral.AbstractOralWebSocketListener;
import cn.xfyun.util.AudioPlayer;
import okhttp3.Response;
import okhttp3.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.security.SignatureException;
import java.util.Objects;
import java.util.UUID;

/**
 * 超拟人的接口demo
 *
 * @author zyding6
 */
public class OralClientApp {
    private static final Logger logger = LoggerFactory.getLogger(OralClientApp.class);
    private static final String appId = PropertiesConfig.getAppId();
    private static final String apiKey = PropertiesConfig.getApiKey();
    private static final String apiSecret = PropertiesConfig.getApiSecret();

    private static String filePath;

    private static String resourcePath;

    static {
        try {
            filePath = "audio/" + UUID.randomUUID() + ".mp3";
            resourcePath = Objects.requireNonNull(OralClientApp.class.getResource("/")).toURI().getPath();
        } catch (URISyntaxException e) {
            logger.error("获取资源路径失败", e);
        }
    }


    public static void main(String[] args) throws MalformedURLException, SignatureException, UnsupportedEncodingException, FileNotFoundException {
        OralClient oralClient = new OralClient.Builder()
                .signature(appId, apiKey, apiSecret)
                .vcn("x4_lingfeizhe_oral")
                .encoding("raw")
                .build();

        // 合成后音频存储路径
        File file = new File(resourcePath + filePath);
        try {

            // 开启语音实时播放
            AudioPlayer audioPlayer = new AudioPlayer();
            audioPlayer.start();

            oralClient.send("我是科大讯飞超拟人, 请问有什么可以帮到您", new AbstractOralWebSocketListener(file) {
                @Override
                public void onSuccess(byte[] bytes) {
                    logger.info("success");
                }

                @Override
                public void onClose(WebSocket webSocket, int code, String reason) {
                    logger.info("关闭连接,code是{},reason:{}", code, reason);
                    audioPlayer.stop();
                    System.exit(0);
                }

                @Override
                public void onFail(WebSocket webSocket, Throwable throwable, Response response) {
                    logger.error(throwable.getMessage());
                    audioPlayer.stop();
                    System.exit(0);
                }

                @Override
                public void onBusinessFail(WebSocket webSocket, OralResponse response) {
                    logger.error(response.toString());
                    audioPlayer.stop();
                    System.exit(0);
                }

                @Override
                public void onPlay(byte[] bytes) {
                    audioPlayer.play(bytes);
                }
            });
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            logger.error("错误码查询链接：https://www.xfyun.cn/document/error-code");
        }
    }
}
