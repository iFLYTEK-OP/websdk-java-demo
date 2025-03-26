package cn.xfyun.demo.face;

import cn.xfyun.api.FaceCompareClient;
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
 * @date 2021/7/21 11:50
 */
public class FaceCompareClientApp {

    private static final String appId = PropertiesConfig.getAppId();
    private static final String apiKey = PropertiesConfig.getApiKey();
    private static final String apiSecret = PropertiesConfig.getApiSecret();

    private static String filePath1 = "xxxxxxxx";
    private static String filePath2 = "xxxxxxxx";
    private static String resourcePath;

    static {
        try {
            resourcePath = FaceCompareClientApp.class.getResource("/").toURI().getPath();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        // 人脸比对
        FaceCompareClient client = new FaceCompareClient
                .Builder(appId, apiKey, apiSecret)
                .build();
        InputStream inputStream = new FileInputStream(new File(resourcePath + filePath1));
        byte[] bytes = IoUtil.readBytes(inputStream);
        String imageBase641 = Base64.getEncoder().encodeToString(bytes);

        InputStream inputStream1 = new FileInputStream(new File(resourcePath + filePath2));
        byte[] bytes1 = IoUtil.readBytes(inputStream1);
        String imageBase642 = Base64.getEncoder().encodeToString(bytes1);
        System.out.println("请求地址：" + client.getHostUrl());
        System.out.println(client.faceCompare(imageBase641, "jpg", imageBase642, "jpg"));
    }
}
