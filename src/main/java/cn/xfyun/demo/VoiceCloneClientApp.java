package cn.xfyun.demo;

import cn.xfyun.api.VoiceCloneClient;
import cn.xfyun.config.PropertiesConfig;
import cn.xfyun.config.VoiceCloneLangEnum;
import cn.xfyun.model.voiceclone.response.VoiceCloneResponse;
import cn.xfyun.service.voiceclone.AbstractVoiceCloneWebSocketListener;
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
import java.util.UUID;

/**
 * 一句话复刻Demo
 *
 * @author zyding6
 */
public class VoiceCloneClientApp {

    private static final Logger logger = LoggerFactory.getLogger(VoiceCloneClientApp.class);
    private static final String appId = PropertiesConfig.getAppId();
    private static final String apiKey = PropertiesConfig.getApiKey();
    private static final String apiSecret = PropertiesConfig.getApiSecret();

    private static String filePath;

    private static String resourcePath;

    static {
        try {
            filePath = "audio/" + UUID.randomUUID() + ".mp3";
            resourcePath = VoiceCloneClientApp.class.getResource("/").toURI().getPath();
        } catch (URISyntaxException e) {
            logger.error("获取资源路径失败", e);
        }
    }


    public static void main(String[] args) throws MalformedURLException, SignatureException, UnsupportedEncodingException, FileNotFoundException {
        VoiceCloneClient voiceCloneClient = new VoiceCloneClient.Builder()
                .signature(appId, apiKey, apiSecret)
                .languageId(VoiceCloneLangEnum.CN.code())
                .resId("123456")
                .build();

        File file = new File(resourcePath + filePath);
        try {
            voiceCloneClient.send("一句话复刻接口将文字信息转化为声音信息", new AbstractVoiceCloneWebSocketListener(file) {
                @Override
                public void onSuccess(byte[] bytes) {
                    logger.info("success");
                }

                @Override
                public void onFail(WebSocket webSocket, Throwable throwable, Response response) {
                    logger.error(throwable.getMessage());
                    System.exit(0);
                }

                @Override
                public void onBusinessFail(WebSocket webSocket, VoiceCloneResponse response) {
                    logger.error(response.toString());
                    System.exit(0);
                }

                @Override
                public void onClose(WebSocket webSocket, int code, String reason) {
                    logger.error("连接关闭，原因：" + reason);
                    System.exit(0);
                }
            });
        } catch (Exception e) {
            logger.error(e.getMessage());
            logger.error("错误码查询链接：https://www.xfyun.cn/document/error-code");
        }
    }
}
