package cn.xfyun.demo;

import cn.xfyun.api.FaceDetectClient;
import cn.xfyun.config.PropertiesConfig;
import sun.misc.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Base64;

/**
 * @author mqgao
 * @version 1.0
 * @date 2021/7/21 11:53
 */
public class FaceDetectClientApp {

    private static final String appId = PropertiesConfig.getAppId();
    private static final String apiKey = PropertiesConfig.getApiKey();
    private static final String apiSecret = PropertiesConfig.getApiSecret();

    private static String filePath = "xxxxxxxx";
    private static String resourcePath;

    static {
        try {
            resourcePath = FaceDetectClientApp.class.getResource("/").toURI().getPath();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        FaceDetectClient client = new FaceDetectClient
                .Builder(appId, apiKey, apiSecret)
                .build();
        InputStream inputStream = new FileInputStream(new File(resourcePath + filePath));
        byte[] bytes = IOUtils.readFully(inputStream, -1, true);
        String imageBase64 = Base64.getEncoder().encodeToString(bytes);
        System.out.println(client.faceContrast(imageBase64, "jpg"));
    }
}
