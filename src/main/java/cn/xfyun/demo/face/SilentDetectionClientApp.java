package cn.xfyun.demo.face;

import cn.xfyun.api.SilentDetectionClient;
import cn.xfyun.config.PropertiesConfig;
import cn.hutool.core.io.IoUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Base64;

/**
 * @author mqgao
 * @version 1.0
 * @date 2021/7/21 11:57
 */
public class SilentDetectionClientApp {

    private static final String appId = PropertiesConfig.getAppId();
    private static final String apiKey = PropertiesConfig.getApiKey();

    private static String filePath = "xxxxxxxx";
    private static String resourcePath;

    static {
        try {
            resourcePath = SilentDetectionClientApp.class.getResource("/").toURI().getPath();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        // 活体检查sensetime
        SilentDetectionClient client = new SilentDetectionClient
                .Builder(appId, apiKey)
                .build();
        InputStream inputStream = new FileInputStream(new File(resourcePath + filePath));
        byte[] bytes = IoUtil.readBytes(inputStream);
        String audioBase64 = Base64.getEncoder().encodeToString(bytes);
        System.out.println("请求地址：" + client.getHostUrl());
        System.out.println(client.silentDetection(audioBase64));
    }
}
