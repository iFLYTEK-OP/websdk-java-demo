package cn.xfyun.demo;

import cn.xfyun.api.TextComplianceClient;
import cn.xfyun.config.PropertiesConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * （text-compliance）文本合规
 * 1、APPID、APISecret、APIKey信息获取：https://console.xfyun.cn/services/text_audit
 * 2、文档地址：https://www.xfyun.cn/doc/nlp/TextModeration/API.html
 */
public class TextComplianceClientApp {

    private static final Logger logger = LoggerFactory.getLogger(TextComplianceClientApp.class);

    private static final String appId = PropertiesConfig.getAppId();
    private static final String apiKey = PropertiesConfig.getApiKey();
    private static final String apiSecret = PropertiesConfig.getApiSecret();


    public static void main(String[] args) throws Exception {
        TextComplianceClient correctionClient = new TextComplianceClient
                .Builder(appId, apiKey, apiSecret)
                .build();
        String result = correctionClient.send("塔利班组织联合东突组织欲图。");
        logger.info("返回结果: {}", result);
    }

}
