package cn.xfyun.demo;

import cn.xfyun.api.ImageGenClient;
import cn.xfyun.config.PropertiesConfig;
import cn.xfyun.model.RoleContent;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * （image-generation）图片生成
 * 1、APPID、APISecret、APIKey信息获取：https://console.xfyun.cn/services/tti
 * 2、文档地址：https://www.xfyun.cn/doc/spark/ImageGeneration.html
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
            resourcePath = ImageGenClientApp.class.getResource("/").toURI().getPath();
        } catch (URISyntaxException e) {
            logger.error("获取资源路径失败", e);
        }
    }


    public static void main(String[] args) throws Exception {
        ImageGenClient client = new ImageGenClient
                .Builder(appId, apiKey, apiSecret)
                .connectTimeout(60000)
                .readTimeout(100000)
                .build();

        List<RoleContent> messages = new ArrayList<>();
        RoleContent roleContent = new RoleContent();
        roleContent.setRole("user");
        roleContent.setContent("帮我画一个小鸟");
        messages.add(roleContent);

        logger.info("请求地址：{}", client.getHostUrl());
        String resp = client.send(messages);
        logger.info("请求返回结果：{}", resp);

        // 结果获取text后解码
        JSONObject obj = JSON.parseObject(resp);
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
    }
}
