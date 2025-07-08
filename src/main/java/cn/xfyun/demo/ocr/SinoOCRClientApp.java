package cn.xfyun.demo.ocr;

import cn.xfyun.api.SinoOCRClient;
import cn.xfyun.config.PropertiesConfig;
import cn.xfyun.util.FileUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

/**
 * （sinosecu-ticket-identify）sinosecu通用票证识别
 * 1、APPID、APISecret、APIKey信息获取：<a href="https://console.xfyun.cn/services/s671ad72d">...</a>
 * 2、文档地址：<a href="https://www.xfyun.cn/doc/words/invoiceIdentification/API.html">...</a>
 */
public class SinoOCRClientApp {

    private static final Logger logger = LoggerFactory.getLogger(SinoOCRClientApp.class);
    private static final String appId = PropertiesConfig.getAppId();
    private static final String apiKey = PropertiesConfig.getApiKey();
    private static final String apiSecret = PropertiesConfig.getApiSecret();
    private static final String filePath = "image/backcard.jpg";
    private static String resourcePath;

    static {
        try {
            resourcePath = Objects.requireNonNull(SinoOCRClientApp.class.getResource("/")).toURI().getPath();
        } catch (URISyntaxException e) {
            logger.error("文件获取资源路径失败", e);
        }
    }

    public static void main(String[] args) throws Exception {
        SinoOCRClient client = new SinoOCRClient
                .Builder(appId, apiKey, apiSecret)
                .build();

        String execute = client.send(FileUtil.fileToBase64(resourcePath + filePath), "jpg");
        logger.info("识别返回结果: {}", execute);
        JSONObject obj = JSON.parseObject(execute);
        String content = obj.getJSONObject("payload").getJSONObject("output_text_result").getString("text");
        byte[] decode = Base64.getDecoder().decode(content);
        String result = new String(decode, StandardCharsets.UTF_8);
        logger.info("base64解码后结果: {}", result);
    }
}
