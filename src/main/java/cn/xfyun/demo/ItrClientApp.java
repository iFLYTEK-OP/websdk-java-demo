package cn.xfyun.demo;

import cn.xfyun.api.ItrClient;
import cn.xfyun.config.ItrEntEnum;
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
 * @date 2021/7/21 11:41
 */
public class ItrClientApp {

    private static final String appId = PropertiesConfig.getAppId();
    private static final String apiKey = PropertiesConfig.getApiKey();
    private static final String apiSecret = PropertiesConfig.getApiSecret();

    private static String filePath = "xxxxxxxx";
    private static String resourcePath;

    static {
        try {
            resourcePath = ItrClientApp.class.getResource("/").toURI().getPath();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        // 拍照速算识别 公式识别
        ItrClient client = new ItrClient
                // 拍照速算识别  ItrEntEnum.MATH_ARITH
                // 公式识别     ItrEntEnum.TEACH_PHOTO_PRINT
                .Builder(appId, apiKey, apiSecret, ItrEntEnum.MATH_ARITH)
                .build();
        InputStream inputStream = new FileInputStream(new File(resourcePath + filePath));
        byte[] bytes = IoUtil.readBytes(inputStream);
        String imageBase64 = Base64.getEncoder().encodeToString(bytes);
        System.out.println("请求地址：" + client.getHostUrl());
        System.out.println(client.itr(imageBase64));
    }
}
