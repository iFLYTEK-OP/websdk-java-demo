package cn.xfyun.demo.ocr;

import cn.xfyun.api.LLMOcrClient;
import cn.xfyun.config.PropertiesConfig;
import cn.xfyun.model.llmocr.LLMOcrParam;
import cn.xfyun.util.FileUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

/**
 * （image-generation）图片生成
 * 1、APPID、APISecret、APIKey信息获取：<a href="https://www.xfyun.cn/services/ocr_model">...</a>
 * 2、文档地址：<a href="https://www.xfyun.cn/doc/words/OCRforLLM/API.html">...</a>
 */
public class LLMOcrClientApp {

    private static final Logger logger = LoggerFactory.getLogger(LLMOcrClientApp.class);
    private static final String appId = PropertiesConfig.getAppId();
    private static final String apiKey = PropertiesConfig.getApiKey();
    private static final String apiSecret = PropertiesConfig.getApiSecret();
    private static String imagePath;
    private static String resourcePath;

    static {
        try {
            imagePath = "image/doc.jpg";
            resourcePath = Objects.requireNonNull(LLMOcrClientApp.class.getResource("/")).toURI().getPath();
        } catch (URISyntaxException e) {
            logger.error("获取资源路径失败", e);
        }
    }

    public static void main(String[] args) throws IOException {
        LLMOcrClient client = new LLMOcrClient
                .Builder(appId, apiKey, apiSecret)
                .build();

        logger.info("请求地址：{}", client.getHostUrl());
        LLMOcrParam param = LLMOcrParam.builder()
                .imageBase64(FileUtil.fileToBase64(resourcePath + imagePath))
                .format("jpg")
                .build();
        String resp = client.send(param);
        JSONObject obj = JSON.parseObject(resp);
        if (obj.getJSONObject("header").getInteger("code") != 0) {
            logger.error("请求失败: {}", resp);
            return;
        }

        // 结果获取text后解码
        String base64;
        try {
            base64 = obj.getJSONObject("payload")
                    .getJSONObject("result")
                    .getString("text");
        } catch (Exception e) {
            throw new RuntimeException("返回结果解析失败", e);
        }
        byte[] decodedBytes = Base64.getDecoder().decode(base64);
        String decodedStr = new String(decodedBytes, StandardCharsets.UTF_8);
        logger.info("解码后结果: {}", decodedStr);
    }
}
