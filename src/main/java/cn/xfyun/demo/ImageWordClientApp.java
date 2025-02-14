package cn.xfyun.demo;

import cn.xfyun.api.ImageWordClient;
import cn.xfyun.config.ImageWordEnum;
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
 * @date 2021/7/21 11:36
 */
public class ImageWordClientApp {

    private static final String appId = PropertiesConfig.getAppId();
    private static final String apiKey = PropertiesConfig.getApiKey();
    private static final String apiSecret = PropertiesConfig.getApiSecret();

    private static String filePath = "image/print.jpg";
    private static String resourcePath;

    static {
        try {
            resourcePath = ImageWordClientApp.class.getResource("/").toURI().getPath();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        ImageWordClient client = new ImageWordClient
                // 身份证识别      ImageWordEnum.IDCARD
                // 营业执照识别    ImageWordEnum.BUSINESS_LICENSE
                // 出租车发票识别  ImageWordEnum.TAXI_INVOICE
                // 火车票识别      ImageWordEnum.TRAIN_TICKET
                // 增值税发票识别  ImageWordEnum.INVOICE
                // 多语种文字识别  ImageWordEnum.PRINTED_WORD
                .Builder(appId, apiKey, apiSecret, ImageWordEnum.PRINTED_WORD)
                .build();
        InputStream inputStream = new FileInputStream(new File(resourcePath + filePath));
        byte[] bytes = IoUtil.readBytes(inputStream);
        String imageBase64 = Base64.getEncoder().encodeToString(bytes);
        System.out.println(client.imageWord(imageBase64, "jpg"));
    }
}
