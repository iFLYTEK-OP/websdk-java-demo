package cn.xfyun.demo;

import cn.xfyun.api.TtsClient;
import cn.xfyun.config.PropertiesConfig;
import cn.xfyun.model.response.TtsResponse;
import cn.xfyun.service.tts.AbstractTtsWebSocketListener;
import okhttp3.Response;
import okhttp3.WebSocket;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.security.SignatureException;

/**
 * TTS ( Text to Speech ):语音合成
 * @author yingpeng
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
            ttsClient.send("语音合成流式接口将文字信息转化为声音信息", new AbstractTtsWebSocketListener(file) {
                @Override
                public void onSuccess(byte[] bytes) {
                }

                @Override
                public void onFail(WebSocket webSocket, Throwable throwable, Response response) {
                    System.out.println(throwable.getMessage());
                }

                @Override
                public void onBusinessFail(WebSocket webSocket, TtsResponse ttsResponse) {
                    System.out.println(ttsResponse.toString());
                }
            });
        }catch (Exception e){
            System.out.println(e.getMessage());
            System.out.println("错误码查询链接：https://www.xfyun.cn/document/error-code");
        }
    }
}
