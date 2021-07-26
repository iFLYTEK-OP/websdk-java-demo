package cn.xfyun.demo;

import cn.xfyun.api.IseHttpClient;
import cn.xfyun.config.IseAueEnum;
import cn.xfyun.config.IseCategoryEnum;
import cn.xfyun.config.IseLanguageEnum;
import cn.xfyun.config.PropertiesConfig;
import sun.misc.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Base64;

/**
 * @author: <flhong2@iflytek.com>
 * @description: 评测(普通版)
 * @version: v1.0
 * @create: 2021-06-11 10:56
 **/
public class IseHttpClientApp {

    private static final String appId = PropertiesConfig.getAppId();
    private static final String apiKey = PropertiesConfig.getApiKey();

    private static String filePath = "audio/cn/read_sentence_cn.pcm";
    private static String resourcePath;

    static {
        try {
            resourcePath = QbhClientApp.class.getResource("/").toURI().getPath();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        // 语音评测（普通版）
        IseHttpClient client =
                new IseHttpClient.Builder(appId, apiKey, IseAueEnum.RAW, IseLanguageEnum.ZH_CN, IseCategoryEnum.READ_SENTENCE)
                .build();
        InputStream inputStream = new FileInputStream(new File(resourcePath + filePath));
        byte[] bytes = IOUtils.readFully(inputStream, -1, true);
        String result = client.send(Base64.getEncoder().encodeToString(bytes), "今天天气怎么样？");
        System.out.println("请求地址：" + client.getHostUrl());
        System.out.println("返回结果: " + result);
    }

}
