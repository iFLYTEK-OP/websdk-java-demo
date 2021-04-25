package cn.xfyun.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;

/**
 * 统一的测试配置获取
 *
 * @author : jun
 * @date : 2021年04月02日
 */
public class PropertiesConfig {
    private static final String appId;
    private static final String apiKey;
    private static final String apiSecret;
    private static final String secretKey;
    private static final String lfasrAppId;
    private static final String iseAppId;
    private static final String iseApiSecret;
    private static final String iseApiKey;

    static {
        Properties properties = new Properties();

        try {
            properties.load(new FileInputStream(PropertiesConfig.class.getResource("/").toURI().getPath() + "test.properties"));
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
        appId = properties.getProperty("appId");
        apiSecret = properties.getProperty("apiSecret");
        apiKey = properties.getProperty("apiKey");

        lfasrAppId = properties.getProperty("lfasrAppId");
        secretKey = properties.getProperty("secretKey");

        iseAppId = properties.getProperty("iseAppId");
        iseApiSecret = properties.getProperty("iseApiSecret");
        iseApiKey = properties.getProperty("iseApiKey");
    }

    public static String getAppId() {
        return appId;
    }

    public static String getSecretKey() {
        return secretKey;
    }

    public static String getLfasrAppId() {
        return lfasrAppId;
    }

    public static String getApiKey() {
        return apiKey;
    }

    public static String getApiSecret() {
        return apiSecret;
    }

    public static String getIseAppId() {
        return iseAppId;
    }

    public static String getIseApiSecret() {
        return iseApiSecret;
    }

    public static String getIseApiKey() {
        return iseApiKey;
    }
}