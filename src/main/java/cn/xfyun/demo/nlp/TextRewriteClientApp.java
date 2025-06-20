package cn.xfyun.demo.nlp;

import cn.xfyun.api.TextRewriteClient;
import cn.xfyun.config.PropertiesConfig;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * （text-rewrite）文本改写
 * 1、APPID、APISecret、APIKey信息获取：<a href="https://console.xfyun.cn/services/text_rewrite">...</a>
 * 2、文档地址：<a href="https://www.xfyun.cn/doc/nlp/textRewriting/API.html">...</a>
 */
public class TextRewriteClientApp {

    private static final Logger logger = LoggerFactory.getLogger(TextRewriteClientApp.class);
    private static final String appId = PropertiesConfig.getAppId();
    private static final String apiKey = PropertiesConfig.getApiKey();
    private static final String apiSecret = PropertiesConfig.getApiSecret();

    public static void main(String[] args) throws Exception {
        TextRewriteClient client = new TextRewriteClient
                .Builder(appId, apiKey, apiSecret)
                .level("L6")
                .build();
        String resp = client.send("随着我国城市化脚步的不断加快，园林工程建设的数量也在不断上升，城市对于园林工程的质量要求也随之上升，" +
                "然而就当前我国园林工程管理的实践而言，就园林工程质量管理这一环节还存在许多不足之处，本文在探讨园林工程质量内涵的基础上，" +
                "深入进行质量管理策略探讨，目的是保障我国园林工程施工质量和提升整体发展效率。", "L6");
        JSONObject obj = JSON.parseObject(resp);

        int code = obj.getJSONObject("header").getIntValue("code");
        if (0 != code) {
            logger.error("请求失败，{}", resp);
            return;
        }
        // 结果获取text后解码
        byte[] decodedBytes = Base64.getDecoder().decode(obj.getJSONObject("payload").getJSONObject("result").getString("text"));
        String decodeRes = new String(decodedBytes, StandardCharsets.UTF_8);
        logger.info("请求地址：{}", client.getHostUrl());
        logger.info("请求返回结果：{}", resp);
        logger.info("文本解码后的结果：{}", decodeRes);
    }
}
