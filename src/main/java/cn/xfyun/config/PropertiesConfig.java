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
    private static final String rtaAPIKey;
    private static final String lfasrSecretKey;
    private static final String sparkBatchKey;
    private static final String maasApiKey;
    private static final String maasResourceId;
    private static final String maasModelId;
    private static final String sparkApiPassword;

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


        rtaAPIKey = properties.getProperty("rtaAPIKey");

        lfasrSecretKey = properties.getProperty("lfasrSecretKey");

        sparkBatchKey = properties.getProperty("sparkBatchKey");

        maasApiKey = properties.getProperty("maasApiKey");

        maasResourceId = properties.getProperty("maasResourceId");

        maasModelId = properties.getProperty("maasModelId");

        sparkApiPassword = properties.getProperty("sparkApiPassword");
    }

    public static String getAppId() {
        return appId;
    }

    public static String getApiKey() {
        return apiKey;
    }

    public static String getApiSecret() {
        return apiSecret;
    }

    public static String getRtaAPIKey() {
        return rtaAPIKey;
    }

    public static String getLfasrSecretKey() {
        return lfasrSecretKey;
    }

    public static String getSparkBatchKey() {
        return sparkBatchKey;
    }

    public static String getMaasApiKey() {
        return maasApiKey;
    }

    public static String getMaasResourceId() {
        return maasResourceId;
    }

    public static String getMaasModelId() {
        return maasModelId;
    }

    public static String getSparkApiPassword() {
        return sparkApiPassword;
    }
}