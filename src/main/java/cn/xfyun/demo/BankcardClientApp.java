package cn.xfyun.demo;

import cn.xfyun.api.BankcardClient;
import cn.xfyun.config.PropertiesConfig;
import sun.misc.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Base64;

/**
 *     银行卡识别
 *
 * @author mqgao
 * @version 1.0
 * @date 2021/7/21 11:27
 */
public class BankcardClientApp {

    private static final String appId = PropertiesConfig.getAppId();
    private static final String apiKey = PropertiesConfig.getApiKey();

    private static String filePath = "xxxxxxx";
    private static String resourcePath;

    static {
        try {
            resourcePath = BankcardClientApp.class.getResource("/").toURI().getPath();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        BankcardClient client = new BankcardClient
                .Builder(appId, apiKey)
                .build();
        InputStream inputStream = new FileInputStream(new File(resourcePath + filePath));
        byte[] bytes = IOUtils.readFully(inputStream, -1, true);
        String imageBase64 = Base64.getEncoder().encodeToString(bytes);
        System.out.println(client.bankcard(imageBase64));
    }
}
