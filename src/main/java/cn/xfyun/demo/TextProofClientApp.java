package cn.xfyun.demo;

import cn.xfyun.api.TextProofreadClient;
import cn.xfyun.config.PropertiesConfig;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * （text-proofread）文本校对
 * 1、APPID、APISecret、APIKey信息获取：https://console.xfyun.cn/services/s37b42a45
 * 2、文档地址：https://www.xfyun.cn/doc/nlp/textCorrectionOfficial/API.html
 */
public class TextProofClientApp {

    private static final Logger logger = LoggerFactory.getLogger(TextProofClientApp.class);

    private static final String appId = PropertiesConfig.getAppId();
    private static final String apiKey = PropertiesConfig.getApiKey();
    private static final String apiSecret = PropertiesConfig.getApiSecret();


    public static void main(String[] args) throws Exception {
        TextProofreadClient client = new TextProofreadClient
                .Builder(appId, apiKey, apiSecret)
                .build();
        String resp = client.send("在干什么你在");
        JSONObject obj = JSON.parseObject(resp);

        // 结果获取text后解码
        byte[] decodedBytes = Base64.getDecoder().decode(obj.getJSONObject("payload").getJSONObject("output_result").getString("text"));
        String decodeRes = new String(decodedBytes, StandardCharsets.UTF_8);
        logger.info("求地址：{}", client.getHostUrl());
        logger.info("请求返回结果：{}", resp);
        logger.info("文本解码后的结果：{}", decodeRes);
    }

}
