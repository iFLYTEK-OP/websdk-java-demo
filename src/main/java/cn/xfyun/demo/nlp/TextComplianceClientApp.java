package cn.xfyun.demo.nlp;

import cn.xfyun.api.TextComplianceClient;
import cn.xfyun.config.PropertiesConfig;
import cn.xfyun.model.compliance.text.TextCompParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * （text-compliance）文本合规
 * 1、APPID、APISecret、APIKey信息获取：<a href="https://console.xfyun.cn/services/text_audit">...</a>
 * 2、文档地址：<a href="https://www.xfyun.cn/doc/nlp/TextModeration/API.html">...</a>
 */
public class TextComplianceClientApp {

    private static final Logger logger = LoggerFactory.getLogger(TextComplianceClientApp.class);
    private static final String appId = PropertiesConfig.getAppId();
    private static final String apiKey = PropertiesConfig.getApiKey();
    private static final String apiSecret = PropertiesConfig.getApiSecret();

    public static void main(String[] args) throws Exception {
        TextComplianceClient client = new TextComplianceClient
                .Builder(appId, apiKey, apiSecret)
                .build();

        TextCompParam param = TextCompParam.builder()
                .content("塔利班组织联合东突组织欲图。").build();
        String result = client.send(param);
        logger.info("返回结果: {}", result);
    }
}
