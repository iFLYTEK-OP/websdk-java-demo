package cn.xfyun.demo;

import cn.xfyun.api.QbhClient;
import cn.xfyun.config.PropertiesConfig;
import cn.xfyun.exception.HttpException;
import sun.misc.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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

    public static void main(String[] args) throws IOException {
        // 歌曲识别
        QbhClient client = new QbhClient.Builder(appId, apiKey)
                .build();
        InputStream inputStream = new FileInputStream(new File(resourcePath + filePath));
        byte[] bytes = IOUtils.readFully(inputStream, -1, true);
        String result = client.send(bytes);
        System.out.println("请求地址：" + client.getHostUrl());
        System.out.println("返回结果: " + result);
    }
}
