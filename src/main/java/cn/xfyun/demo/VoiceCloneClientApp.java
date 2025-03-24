package cn.xfyun.demo;

import cn.xfyun.api.VoiceCloneClient;
import cn.xfyun.config.AudioPlayer;
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
            filePath = "audio/voiceclone_" + UUID.randomUUID() + ".mp3";
            resourcePath = VoiceCloneClientApp.class.getResource("/").toURI().getPath();
        } catch (URISyntaxException e) {
            logger.error("获取资源路径失败", e);
        }
    }


    public static void main(String[] args) throws MalformedURLException, SignatureException, UnsupportedEncodingException, FileNotFoundException {
        String text = "欢迎使用本语音合成测试文本，本测试旨在全面检验语音合成系统在准确性、流畅性以及自然度等多方面的性能表现。";
        VoiceCloneClient voiceCloneClient = new VoiceCloneClient.Builder()
                .signature(appId, apiKey, apiSecret)
                .languageId(VoiceCloneLangEnum.CN.code())
                .encoding("raw")
                .resId("替换成你的一句话生成的id")
                .build();

        File file = new File(resourcePath + filePath);
        try {
            // 开启语音实时播放
            AudioPlayer audioPlayer = new AudioPlayer();
            audioPlayer.start();
            voiceCloneClient.send(text, new AbstractVoiceCloneWebSocketListener(file) {
                @Override
                public void onSuccess(byte[] bytes) {
                    logger.info("success");
                }

                @Override
                public void onFail(WebSocket webSocket, Throwable throwable, Response response) {
                    logger.error(throwable.getMessage());
                    audioPlayer.stop();
                    System.exit(0);
                }

                @Override
                public void onBusinessFail(WebSocket webSocket, VoiceCloneResponse response) {
                    logger.error(response.toString());
                    audioPlayer.stop();
                    System.exit(0);
                }

                @Override
                public void onClose(WebSocket webSocket, int code, String reason) {
                    logger.info("连接关闭，原因：" + reason);
                    audioPlayer.stop();
                    System.exit(0);
                }

                @Override
                public void onPlay(byte[] bytes) {
                    audioPlayer.play(bytes);
                }
            });
        } catch (Exception e) {
            logger.error(e.getMessage());
            logger.error("错误码查询链接：https://www.xfyun.cn/document/error-code");
        }
    }
}
