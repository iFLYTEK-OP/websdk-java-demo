package cn.xfyun.demo;

import cn.xfyun.api.FaceVerificationClient;
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
 * @date 2021/7/21 11:55
 */
public class FaceVerificationClientApp {

    private static final String appId = PropertiesConfig.getAppId();
    private static final String apiKey = PropertiesConfig.getApiKey();

    private static String filePath1 = "xxxxxxxx";
    private static String filePath2 = "xxxxxxxx";
    private static String resourcePath;

    static {
        try {
            resourcePath = FaceVerificationClientApp.class.getResource("/").toURI().getPath();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        // 人脸比对sensetime
        FaceVerificationClient client = new FaceVerificationClient
                .Builder(appId, apiKey)
                .build();
        InputStream inputStream1 = new FileInputStream(new File(resourcePath + filePath1));
        byte[] bytes1 = IoUtil.readBytes(inputStream1);
        String imageBase641 = Base64.getEncoder().encodeToString(bytes1);

        InputStream inputStream2 = new FileInputStream(new File(resourcePath + filePath2));
        byte[] bytes2 = IoUtil.readBytes(inputStream2);
        String imageBase642 = Base64.getEncoder().encodeToString(bytes2);
        System.out.println("请求地址：" + client.getHostUrl());
        System.out.println(client.compareFace(imageBase641, imageBase642));
    }
}
