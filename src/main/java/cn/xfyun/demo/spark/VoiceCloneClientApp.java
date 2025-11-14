package cn.xfyun.demo.spark;

import cn.xfyun.api.VoiceCloneV2Client;
import cn.xfyun.config.PropertiesConfig;
import cn.xfyun.config.VoiceCloneLangEnum;
import cn.xfyun.config.VoiceStyleEnum;
import cn.xfyun.model.voiceclone.VoiceCloneParam;
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
 * （voice-clone）一句话复刻(美化版、标准版)
 * 1、APPID、APISecret、APIKey信息获取：<a href="https://console.xfyun.cn/services/oneSentence">...</a>
 * 2、文档地址：<a href="https://www.xfyun.cn/doc/spark/reproduction.html">...</a>
 */
public class VoiceCloneClientApp {

    private static final Logger logger = LoggerFactory.getLogger(VoiceCloneClientApp.class);
    private static final String appId = PropertiesConfig.getAppId();
    private static final String apiKey = PropertiesConfig.getApiKey();
    private static final String apiSecret = PropertiesConfig.getApiSecret();
    private static final String assetId = "您一句话复刻训练生成的声纹ID(标准版本 x5_clone)";
    private static final String omni_assetId = "您一句话复刻训练生成的声纹ID(美化版本 x6_clone)";
    private static String filePath;
    private static String resourcePath;

    static {
        try {
            filePath = "audio/voiceclone_" + UUID.randomUUID() + ".pcm";
            resourcePath = Objects.requireNonNull(VoiceCloneClientApp.class.getResource("/")).toURI().getPath();
        } catch (URISyntaxException e) {
            logger.error("获取资源路径失败", e);
        }
    }

    public static void main(String[] args) throws MalformedURLException, SignatureException, UnsupportedEncodingException, FileNotFoundException {
        String text = "欢迎使用本语音合成测试文本，本测试旨在全面检验语音合成系统在准确性、流畅性以及自然度等多方面的性能表现。";

        // 标准版
        // general(text);

        // 美化版
        omni(text);
    }

    private static void general(String text) {
        VoiceCloneV2Client voiceCloneClient = new VoiceCloneV2Client.Builder()
                .signature(appId, apiKey, apiSecret)
                .encoding("raw")
                .sampleRate(16000)
                .build();

        VoiceCloneParam param = VoiceCloneParam.builder()
                .text(text)
                .vcn("x5_clone")
                .languageId(VoiceCloneLangEnum.CN.code())
                .resId(assetId)
                .build();

        File file = new File(resourcePath + filePath);
        try {

            // 开启语音实时播放
            AudioPlayer audioPlayer = new AudioPlayer();
            audioPlayer.start();

            voiceCloneClient.send(param, getListener(file, audioPlayer));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            logger.error("错误码查询链接：https://www.xfyun.cn/document/error-code");
        }
    }

    private static void omni(String text) {
        VoiceCloneV2Client voiceCloneClient = new VoiceCloneV2Client.Builder()
                .signature(appId, apiKey, apiSecret)
                .encoding("raw")
                .sampleRate(16000)
                .build();

        VoiceCloneParam param = VoiceCloneParam.builder()
                .text(text)
                .vcn("x6_clone")
                .resId(omni_assetId)
                .style(VoiceStyleEnum.NEWS.getCode())
                .build();

        File file = new File(resourcePath + filePath);
        try {

            // 开启语音实时播放
            AudioPlayer audioPlayer = new AudioPlayer();
            audioPlayer.start();

            voiceCloneClient.send(param, getListener(file, audioPlayer));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            logger.error("错误码查询链接：https://www.xfyun.cn/document/error-code");
        }
    }

    private static AbstractVoiceCloneWebSocketListener getListener(File file, AudioPlayer audioPlayer) throws FileNotFoundException {
        return new AbstractVoiceCloneWebSocketListener(file) {
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
        };
    }
}
