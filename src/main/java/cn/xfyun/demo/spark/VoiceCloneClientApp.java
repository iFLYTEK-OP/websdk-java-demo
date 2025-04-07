package cn.xfyun.demo.spark;

import cn.xfyun.api.VoiceCloneClient;
import cn.xfyun.config.PropertiesConfig;
import cn.xfyun.config.VoiceCloneLangEnum;
import cn.xfyun.model.voiceclone.response.VoiceCloneResponse;
import cn.xfyun.service.voiceclone.AbstractVoiceCloneWebSocketListener;
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
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

/**
 * （voice-clone）一句话复刻
 * 1、APPID、APISecret、APIKey信息获取：<a href="https://console.xfyun.cn/services/oneSentence">...</a>
 * 2、文档地址：<a href="https://www.xfyun.cn/doc/spark/reproduction.html">...</a>
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
            resourcePath = Objects.requireNonNull(VoiceCloneClientApp.class.getResource("/")).toURI().getPath();
        } catch (URISyntaxException e) {
            logger.error("获取资源路径失败", e);
        }
    }

    public static void main(String[] args) throws MalformedURLException, SignatureException, UnsupportedEncodingException, FileNotFoundException {
        String text = "欢迎使用本语音合成测试文本，本测试旨在全面检验语音合成系统在准确性、流畅性以及自然度等多方面的性能表现。";
        VoiceCloneClient voiceCloneClient = new VoiceCloneClient.Builder()
                .signature("您的一句话复刻生成的声纹Id", VoiceCloneLangEnum.CN, appId, apiKey, apiSecret)
                .encoding("raw")
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
                public void onMessage(WebSocket webSocket, String text) {
                    super.onMessage(webSocket, text);
                    VoiceCloneResponse resp = JSON.fromJson(text, VoiceCloneResponse.class);
                    if (resp != null) {
                        VoiceCloneResponse.PayloadBean payload = resp.getPayload();

                        if (resp.getHeader().getCode() != 0) {
                            onBusinessFail(webSocket, resp);
                        }

                        if (null != payload && null != payload.getAudio()) {
                            String result = payload.getAudio().getAudio();
                            if (result != null) {
                                byte[] audio = Base64.getDecoder().decode(result);
                                audioPlayer.play(audio);
                            }
                        }
                    }
                }
            });
        } catch (Exception e) {
            logger.error(e.getMessage());
            logger.error("错误码查询链接：https://www.xfyun.cn/document/error-code");
        }
    }
}
