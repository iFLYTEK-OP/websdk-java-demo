package cn.xfyun.demo.ocr;

import cn.xfyun.api.TicketOCRClient;
import cn.xfyun.config.DocumentType;
import cn.xfyun.config.PropertiesConfig;
import cn.xfyun.model.ticket.TicketOCRParam;
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
 * （ticket-identify）通用票证识别
 * 1、APPID、APISecret、APIKey信息获取：<a href="https://console.xfyun.cn/services/inv">...</a>
 * 2、文档地址：<a href="https://www.xfyun.cn/doc/words/TicketIdentification/API.html">...</a>
 */
public class TicketOCRClientApp {

    private static final Logger logger = LoggerFactory.getLogger(TicketOCRClientApp.class);
    private static final String appId = PropertiesConfig.getAppId();
    private static final String apiKey = PropertiesConfig.getApiKey();
    private static final String apiSecret = PropertiesConfig.getApiSecret();
    private static final String filePath = "image/backcard.jpg";
    private static String resourcePath;

    static {
        try {
            resourcePath = Objects.requireNonNull(TicketOCRClientApp.class.getResource("/")).toURI().getPath();
        } catch (URISyntaxException e) {
            logger.error("文件获取资源路径失败", e);
        }
    }

    public static void main(String[] args) throws Exception {
        TicketOCRClient client = new TicketOCRClient
                .Builder(appId, apiKey, apiSecret)
                .build();

        DocumentType type = DocumentType.BANK_CARD;
        TicketOCRParam param = TicketOCRParam.builder()
                .documentType(type)
                .imageBase64(FileUtil.fileToBase64(resourcePath + filePath))
                .imageFormat("jpg")
                .build();
        String execute = client.send(param);
        logger.info("{} 识别返回结果: {}", type.getDesc(), execute);
        JSONObject obj = JSON.parseObject(execute);
        String content = obj.getJSONObject("payload").getJSONObject("result").getString("text");
        byte[] decode = Base64.getDecoder().decode(content);
        String result = new String(decode, StandardCharsets.UTF_8);
        logger.info("base64解码后结果: {}", result);
    }
}
