package cn.xfyun.demo;

import cn.xfyun.api.TupApiClient;
import cn.xfyun.config.PropertiesConfig;
import cn.xfyun.config.TupApiEnum;
import cn.hutool.core.io.IoUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URISyntaxException;

/**
 * @author mqgao
 * @version 1.0
 * @date 2021/7/21 11:58
 */
public class TupApiClientApp {

    private static final String appId = PropertiesConfig.getAppId();
    private static final String apiKey = PropertiesConfig.getApiKey();

    private static String filePath = "xxxxxxxx";
    private static String resourcePath;

    static {
        try {
            resourcePath = TupApiClientApp.class.getResource("/").toURI().getPath();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) throws Exception {
        TupApiClient client = new TupApiClient
                // 年龄      TupApiEnum.AGE
                // 性别      TupApiEnum.SEX
                // 表情      TupApiEnum.EXPRESSION
                // 颜值      TupApiEnum.FACE_SCORE
                .Builder(appId, apiKey, TupApiEnum.AGE)
                .build();
        InputStream inputStream = new FileInputStream(new File(resourcePath + filePath));
        byte[] bytes = IoUtil.readBytes(inputStream);
        System.out.println("请求地址：" + client.getHostUrl());
        System.out.println(client.recognition("测试", bytes));
    }
}
