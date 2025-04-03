package cn.xfyun.demo.speech;

import cn.xfyun.api.TtsClient;
import cn.xfyun.config.PropertiesConfig;
import cn.xfyun.model.response.TtsResponse;
import cn.xfyun.service.tts.AbstractTtsWebSocketListener;
import cn.xfyun.util.AudioPlayer;
import okhttp3.Response;
import okhttp3.WebSocket;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.security.SignatureException;
import java.util.Base64;

/**
 * TTS ( Text to Speech ):语音合成
 1、APPID、APISecret、APIKey信息获取：https://console.xfyun.cn/services/tts
 2、文档地址：https://www.xfyun.cn/doc/tts/online_tts/API.html
 */
public class TtsClientApp {

    private static final String appId = PropertiesConfig.getAppId();
    private static final String apiKey = PropertiesConfig.getApiKey();
    private static final String apiSecret = PropertiesConfig.getApiSecret();

    private static String filePath = "audio/tts1.mp3";
    private static String resourcePath = "src/main/resources/";

    public static void main(String[] args) throws MalformedURLException, SignatureException, UnsupportedEncodingException, FileNotFoundException {
        TtsClient ttsClient = new TtsClient.Builder()
                .signature(appId, apiKey, apiSecret)
                .build();

        File file = new File(resourcePath + filePath);
        try {

            // 开启语音实时播放
            AudioPlayer audioPlayer = new AudioPlayer();
            audioPlayer.start();

            ttsClient.send("语音合成流式接口将文字信息转化为声音信息", new AbstractTtsWebSocketListener(file) {
                @Override
                public void onSuccess(byte[] bytes) {
                }

                @Override
                public void onFail(WebSocket webSocket, Throwable throwable, Response response) {
                    System.out.println(throwable.getMessage());
                    audioPlayer.stop();
                }

                @Override
                public void onBusinessFail(WebSocket webSocket, TtsResponse ttsResponse) {
                    System.out.println(ttsResponse.toString());
                    audioPlayer.stop();
                }

                @Override
                public void onMessage(WebSocket webSocket, String text) {
                    super.onMessage(webSocket, text);
                    TtsResponse resp = JSON.fromJson(text, TtsResponse.class);
                    if (resp != null) {
                        if (resp.getCode() != 0) {
                            onBusinessFail(webSocket, resp);
                        }
                        if (resp.getData() != null) {
                            String result = resp.getData().getAudio();
                            if (result != null) {
                                byte[] audio = Base64.getDecoder().decode(result);
                                audioPlayer.play(audio);
                            }
                        }
                    }
                }
            });
        }catch (Exception e){
            System.out.println(e.getMessage());
            System.out.println("错误码查询链接：https://www.xfyun.cn/document/error-code");
        }
    }
}
