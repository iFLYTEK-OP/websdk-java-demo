package cn.xfyun.demo;

import cn.xfyun.api.TextCheckClient;
import cn.xfyun.config.PropertiesConfig;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * （text-check）文本纠错
 * 1、APPID、APISecret、APIKey信息获取：https://console.xfyun.cn/services/text_check
 * 2、文档地址：https://www.xfyun.cn/doc/nlp/textCorrection/API.html
 */
public class TextCheckClientApp {

    private static final Logger logger = LoggerFactory.getLogger(TextCheckClientApp.class);

    private static final String appId = PropertiesConfig.getAppId();
    private static final String apiKey = PropertiesConfig.getApiKey();
    private static final String apiSecret = PropertiesConfig.getApiSecret();


    public static void main(String[] args) throws Exception {
        TextCheckClient client = new TextCheckClient
                .Builder(appId, apiKey, apiSecret)
                .build();
        logger.info("请求地址：{}", client.getHostUrl());
        String result = client.send("画蛇天足");
        logger.info("请求结果：{}", result);
        JSONObject obj = JSON.parseObject(result);
        String base64 = obj.getJSONObject("payload").getJSONObject("result").getString("text");
        byte[] decode = Base64.getDecoder().decode(base64);
        String decodeStr = new String(decode, StandardCharsets.UTF_8);
        logger.info("解码后结果：{}", decodeStr);
    }

}
