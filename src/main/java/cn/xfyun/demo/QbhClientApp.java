package cn.xfyun.demo;

import cn.xfyun.api.QbhClient;
import cn.xfyun.config.PropertiesConfig;
import cn.xfyun.exception.HttpException;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 * @author: <flhong2@iflytek.com>
 * @description: 歌曲识别
 * @version: v1.0
 * @create: 2021-06-11 10:08
 **/
public class QbhClientApp {

    private static final String appId = PropertiesConfig.getAppId();
    private static final String apiKey = PropertiesConfig.getApiKey();

    private static String filePath = "audio/audio_qbh.wav";
    private static String resourcePath;

    static {
        try {
            resourcePath = QbhClientApp.class.getResource("/").toURI().getPath();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException, HttpException {
        QbhClient qbhClientApp = new QbhClient.Builder()
                .appId(appId).apiKey(apiKey)
                .engineType("afs")
                .build();
        String result = qbhClientApp.send(new File(resourcePath + filePath));
        System.out.println("返回结果: " + result);
    }
}
