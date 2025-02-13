package cn.xfyun.demo;

import cn.hutool.core.io.IoUtil;
import cn.xfyun.api.AntiSpoofClient;
import cn.xfyun.config.PropertiesConfig;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Base64;

/**
 * @author mqgao
 * @version 1.0
 * @date 2021/7/21 11:46
 */
public class AntiSpoofClientApp {

    private static final String appId = PropertiesConfig.getAppId();
    private static final String apiKey = PropertiesConfig.getApiKey();
    private static final String apiSecret = PropertiesConfig.getApiSecret();

    private static String filePath = "xxxxxxxx";
    private static String resourcePath;

    static {
        try {
            resourcePath = AntiSpoofClientApp.class.getResource("/").toURI().getPath();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        // 静默活体检测
        AntiSpoofClient client = new AntiSpoofClient
                .Builder(appId, apiKey, apiSecret)
                .build();
        InputStream inputStream = new FileInputStream(new File(resourcePath + filePath));
        byte[] bytes = IoUtil.readBytes(inputStream);
        String imageBase64 = Base64.getEncoder().encodeToString(bytes);
        System.out.println("请求地址：" + client.getHostUrl());
        System.out.println(client.faceContrast(imageBase64, "jpg"));
    }
}
