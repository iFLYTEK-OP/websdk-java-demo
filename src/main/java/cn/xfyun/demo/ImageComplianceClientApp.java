package cn.xfyun.demo;

import cn.xfyun.api.ImageComplianceClient;
import cn.xfyun.config.ModeType;
import cn.xfyun.config.PropertiesConfig;
import cn.xfyun.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;

/**
 * （image-compliance）图片合规
 * 1、APPID、APISecret、APIKey信息获取：https://console.xfyun.cn/services/image_audit
 * 2、文档地址：https://www.xfyun.cn/doc/nlp/ImageModeration/API.html
 */
public class ImageComplianceClientApp {

    private static final Logger logger = LoggerFactory.getLogger(ImageComplianceClientApp.class);

    private static final String appId = PropertiesConfig.getAppId();
    private static final String apiKey = PropertiesConfig.getApiKey();
    private static final String apiSecret = PropertiesConfig.getApiSecret();

    private static String resourcePath;

    // 待检测图片路径
    private static String imagePath;

    // 待检测图片地址
    private static String imageUrl;

    static {
        try {
            resourcePath = OralClientApp.class.getResource("/").toURI().getPath();
            imagePath = "image/nationalflag.png";
            imageUrl = "http://baidu.com/1.jpg";
        } catch (URISyntaxException e) {
            logger.error("文件路径加载异常", e);
        }
    }


    public static void main(String[] args) throws Exception {
        ImageComplianceClient correctionClient = new ImageComplianceClient
                .Builder(appId, apiKey, apiSecret)
                .build();

        String pathResp = correctionClient.send(FileUtils.fileToBase64(resourcePath + imagePath), ModeType.BASE64.getValue());
        logger.info("图片地址返回结果: {}", pathResp);

        // String urlResp = correctionClient.send(imageUrl, ModeType.LINK.getValue());
        // logger.info("图片链接返回结果: {}", urlResp);
    }

}
