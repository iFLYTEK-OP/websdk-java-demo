package cn.xfyun.demo.spark;

import cn.xfyun.api.ResumeGenClient;
import cn.xfyun.config.PropertiesConfig;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * （text-proofread）简历生成
 * 1、APPID、APISecret、APIKey信息获取：<a href="https://console.xfyun.cn/services/s73f4add9">...</a>
 * 2、文档地址：<a href="https://www.xfyun.cn/doc/spark/resume.html">...</a>
 */
public class ResumeGenClientApp {

    private static final Logger logger = LoggerFactory.getLogger(ResumeGenClientApp.class);
    private static final String appId = PropertiesConfig.getAppId();
    private static final String apiKey = PropertiesConfig.getApiKey();
    private static final String apiSecret = PropertiesConfig.getApiSecret();

    public static void main(String[] args) throws IOException {
        ResumeGenClient client = new ResumeGenClient
                .Builder(appId, apiKey, apiSecret)
                .build();

        logger.info("请求地址：{}", client.getHostUrl());
        AtomicBoolean isEnd = new AtomicBoolean(false);
        logHandler(isEnd);
        String resp = client.send("我是一名从业5年的java开发程序员, 今年25岁, 邮箱是xxx@qq.com , 电话13000000000, 性别男 , 就业地址合肥, 期望薪资20k , 主要从事AI大模型相关的项目经历");
        isEnd.set(true);
        logger.info("请求返回结果：{}", resp);

        JSONObject obj = JSONObject.parseObject(resp);
        int code = obj.getJSONObject("header").getIntValue("code");
        if (0 == code) {
            // 结果获取text后解码
            byte[] decodedBytes = Base64.getDecoder().decode(obj.getJSONObject("payload").getJSONObject("resData").getString("text"));
            String decodeRes = new String(decodedBytes, StandardCharsets.UTF_8);
            logger.info("文本解码后的结果：{}", decodeRes);
        } else {
            logger.error("code=>{}，error=>{}", code, obj.getJSONObject("header").getString("message"));
        }
    }

    private static void logHandler(AtomicBoolean isEnd) {
        // 多语种没有流式返回会开启线程动态打印日志
        Thread loading = new Thread(() -> {
            while (!isEnd.get()) {
                logger.info("简历生成中...");
                try {
                    TimeUnit.MILLISECONDS.sleep(2500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        loading.setName("Logger-ResumeGenClientApp-Thread");
        loading.start();
    }
}
