package cn.xfyun.demo.spark;

import cn.xfyun.api.ImageGenClient;
import cn.xfyun.config.PropertiesConfig;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

/**
 * （image-generation）图片生成
 * 1、APPID、APISecret、APIKey信息获取：<a href="https://console.xfyun.cn/services/tti">...</a>
 * 2、文档地址：<a href="https://www.xfyun.cn/doc/spark/ImageGeneration.html">...</a>
 */
public class ImageGenClientApp {

    private static final Logger logger = LoggerFactory.getLogger(ImageGenClientApp.class);
    private static final String appId = PropertiesConfig.getAppId();
    private static final String apiKey = PropertiesConfig.getApiKey();
    private static final String apiSecret = PropertiesConfig.getApiSecret();
    private static String imagePath;
    private static String resourcePath;

    static {
        try {
            imagePath = "image/gen_" + UUID.randomUUID() + ".png";
            resourcePath = Objects.requireNonNull(ImageGenClientApp.class.getResource("/")).toURI().getPath();
        } catch (URISyntaxException e) {
            logger.error("获取资源路径失败", e);
        }
    }

    public static void main(String[] args) throws IOException {
        ImageGenClient client = new ImageGenClient
                .Builder(appId, apiKey, apiSecret)
                .build();

        logger.info("请求地址：{}", client.getHostUrl());
        String resp = client.send("帮我画一个小鸟");
        // logger.info("请求结果：{}", resp);

        try {
            JSONObject obj = JSON.parseObject(resp);
            if (obj.getJSONObject("header").getInteger("code") != 0) {
                logger.error("请求失败: {}", resp);
                return;
            }
            // 结果获取text后解码
            String base64 = obj.getJSONObject("payload")
                    .getJSONObject("choices")
                    .getJSONArray("text")
                    .getJSONObject(0)
                    .getString("content");

            byte[] decodedBytes = Base64.getDecoder().decode(base64);
            // base64解码后的图片字节数组写入文件
            try (FileOutputStream imageOutFile = new FileOutputStream(resourcePath + imagePath)) {
                // 将字节数组写入文件
                imageOutFile.write(decodedBytes);
                logger.info("图片已成功保存到: {}", resourcePath + imagePath);
            } catch (IOException e) {
                logger.error("保存图片时出错: {}", e.getMessage(), e);
            }
        } catch (Exception e) {
            logger.error("解析返回结果失败", e);
        }
    }
}
