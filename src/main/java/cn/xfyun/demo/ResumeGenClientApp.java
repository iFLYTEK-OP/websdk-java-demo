package cn.xfyun.demo;

import cn.xfyun.api.ResumeGenClient;
import cn.xfyun.config.PropertiesConfig;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * （text-proofread）简历生成
 * 1、APPID、APISecret、APIKey信息获取：https://console.xfyun.cn/services/s73f4add9
 * 2、文档地址：https://www.xfyun.cn/doc/spark/resume.html
 */
public class ResumeGenClientApp {
    private static final Logger logger = LoggerFactory.getLogger(ResumeGenClientApp.class);
    private static final String appId = PropertiesConfig.getAppId();
    private static final String apiKey = PropertiesConfig.getApiKey();
    private static final String apiSecret = PropertiesConfig.getApiSecret();


    public static void main(String[] args) throws Exception {
        ResumeGenClient client = new ResumeGenClient
                .Builder(appId, apiKey, apiSecret)
                .connectTimeout(70000)
                .readTimeout(10000)
                .build();

        logger.info("请求地址：{}", client.getHostUrl());
        String resp = client.send("我是一名从业5年的java开发程序员, 今年25岁, 邮箱是xxx@qq.com , 电话13000000000, 性别男 , 就业地址合肥, 期望薪资20k , 主要从事AI大模型相关的项目经历");
        logger.info("请求返回结果：{}", resp);

        // 结果获取text后解码
        JSONObject obj = JSON.parseObject(resp);
        byte[] decodedBytes = Base64.getDecoder().decode(obj.getJSONObject("payload").getJSONObject("resData").getString("text"));
        String decodeRes = new String(decodedBytes, StandardCharsets.UTF_8);
        logger.info("文本解码后的结果：{}", decodeRes);
    }
}
