package cn.xfyun.demo.ocr;

import cn.xfyun.api.IntsigOcrClient;
import cn.xfyun.config.IntsigRecgEnum;
import cn.xfyun.config.PropertiesConfig;
import cn.hutool.core.io.IoUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Base64;

/**
 * @author mqgao
 * @version 1.0
 * @date 2021/7/21 11:39
 */
public class IntsigOcrClientApp {

    private static final String appId = PropertiesConfig.getAppId();
    private static final String apiKey = PropertiesConfig.getApiKey();

    private static String filePath = "xxxxxxxx";
    private static String resourcePath;

    static {
        try {
            resourcePath = IntsigOcrClientApp.class.getResource("/").toURI().getPath();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        IntsigOcrClient client = new IntsigOcrClient
                // 身份证识别             IntsigRecgEnum.IDCARD
                // 营业执照识别           IntsigRecgEnum.BUSINESS_LICENSE
                // 增值税发票识别         IntsigRecgEnum.INVOICE
                // 印刷文字识别（多语种）  IntsigRecgEnum.RECOGNIZE_DOCUMENT
                .Builder(appId, apiKey, IntsigRecgEnum.IDCARD)
                .build();
        InputStream inputStream = new FileInputStream(new File(resourcePath + filePath));
        byte[] bytes = IoUtil.readBytes(inputStream);
        String imageBase64 = Base64.getEncoder().encodeToString(bytes);
        System.out.println("请求地址：" + client.getHostUrl());
        System.out.println(client.intsigRecg(imageBase64));
    }
}
