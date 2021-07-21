package cn.xfyun.demo;

import cn.xfyun.api.WatermarkVerificationClient;
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
 * @date 2021/7/21 12:01
 */
public class WatermarkVerificationClientApp {

    private static final String appId = PropertiesConfig.getAppId();
    private static final String apiKey = PropertiesConfig.getApiKey();

    private static String filePath1 = "xxxxxxxx";
    private static String filePath2 = "xxxxxxxx";
    private static String resourcePath;

    static {
        try {
            resourcePath = WatermarkVerificationClientApp.class.getResource("/").toURI().getPath();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        WatermarkVerificationClient client = new WatermarkVerificationClient
                .Builder(appId, apiKey)
                .build();
        InputStream inputStream1 = new FileInputStream(new File(resourcePath + filePath1));
        byte[] bytes1 = IOUtils.readFully(inputStream1, -1, true);
        String imageBase641 = Base64.getEncoder().encodeToString(bytes1);

        InputStream inputStream2 = new FileInputStream(new File(resourcePath + filePath2));
        byte[] bytes2 = IOUtils.readFully(inputStream2, -1, true);
        String imageBase642 = Base64.getEncoder().encodeToString(bytes2);
        System.out.println(client.compare(imageBase641, imageBase642));
    }
}