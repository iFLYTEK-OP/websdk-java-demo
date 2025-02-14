package cn.xfyun.demo;

import cn.xfyun.api.JDOcrClient;
import cn.xfyun.config.JDRecgEnum;
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
 * @date 2021/7/21 11:43
 */
public class JDOcrClientApp {

    private static final String appId = PropertiesConfig.getAppId();
    private static final String apiKey = PropertiesConfig.getApiKey();
    private static final String apiSecret = PropertiesConfig.getApiSecret();

    private static String filePath = "xxxxxxxx";
    private static String resourcePath;

    static {
        try {
            resourcePath = JDOcrClientApp.class.getResource("/").toURI().getPath();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        JDOcrClient client = new JDOcrClient
                // 行驶证识别  JDRecgEnum.JD_OCR_VEHICLE
                // 驾驶证识别  JDRecgEnum.JD_OCR_DRIVER
                // 车牌识别    JDRecgEnum.JD_OCR_CAR
                .Builder(appId, apiKey, apiSecret, JDRecgEnum.JD_OCR_VEHICLE)
                .build();
        InputStream inputStream = new FileInputStream(new File(resourcePath + filePath));
        byte[] bytes = IoUtil.readBytes(inputStream);
        String imageBase64 = Base64.getEncoder().encodeToString(bytes);
        System.out.println("请求地址：" + client.getHostUrl());
        System.out.println(client.handle(imageBase64, "jpg"));
    }
}
