package cn.xfyun.demo;

import cn.xfyun.api.CommonIseClient;
import cn.xfyun.config.PropertiesConfig;
import cn.xfyun.exception.HttpException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

/**
 * @author: <flhong2@iflytek.com>
 * @description: 评测(普通版)
 * @version: v1.0
 * @create: 2021-06-11 10:56
 **/
public class CommonIseApp {

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

    public static void main(String[] args) throws IOException, HttpException {
        CommonIseClient commonIseClient = new CommonIseClient.Builder()
                .appId(appId).apiKey(apiKey).aue("raw").speexSize("70")
                .resultLevel("simple").language("zh_cn").category("read_sentence")
                .extraAbility("multi_dimension").text("今天天气怎么样？")
                .build();
        InputStream inputStream = new FileInputStream(new File(resourcePath + filePath));
        String result = commonIseClient.send(inputStream);
        System.out.println("返回结果: " + result);
    }

}
