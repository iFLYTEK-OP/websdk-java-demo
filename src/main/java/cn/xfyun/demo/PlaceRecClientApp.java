package cn.xfyun.demo;

import cn.xfyun.api.PlaceRecClient;
import cn.xfyun.config.PropertiesConfig;
import sun.misc.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Base64;

public class PlaceRecClientApp {

    private static final String appId = PropertiesConfig.getAppId();
    private static final String apiKey = PropertiesConfig.getApiKey();
    private static final String apiSecret = PropertiesConfig.getApiSecret();

    private static String filePath = "xxxxxxxx";
    private static String resourcePath;

    static {
        try {
            resourcePath = PlaceRecClientApp.class.getResource("/").toURI().getPath();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        // 场所识别
        PlaceRecClient client = new PlaceRecClient
                .Builder(appId, apiKey, apiSecret)
                .build();
        InputStream inputStream = new FileInputStream(new File(resourcePath + filePath));
        byte[] imageByteArray = IOUtils.readFully(inputStream, -1, true);
        String imageBase64 = Base64.getEncoder().encodeToString(imageByteArray);
        System.out.println(client.send(imageBase64, "jpg"));
    }
}
