package cn.xfyun.demo.nlp;

import cn.xfyun.api.ImageComplianceClient;
import cn.xfyun.config.ModeType;
import cn.xfyun.config.PropertiesConfig;
import cn.xfyun.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.util.Objects;

/**
 * （image-compliance）图片合规
 * 1、APPID、APISecret、APIKey信息获取：<a href="https://console.xfyun.cn/services/image_audit">...</a>
 * 2、文档地址：<a href="https://www.xfyun.cn/doc/nlp/ImageModeration/API.html">...</a>
 */
public class ImageComplianceClientApp {

    private static final Logger logger = LoggerFactory.getLogger(ImageComplianceClientApp.class);
    private static final String appId = PropertiesConfig.getAppId();
    private static final String apiKey = PropertiesConfig.getApiKey();
    private static final String apiSecret = PropertiesConfig.getApiSecret();
    private static String resourcePath;
    /**
     * 待检测图片路径
     */
    private static String imagePath;
    /**
     * 待检测图片链接
     */
    private static String imageUrl;

    static {
        try {
            resourcePath = Objects.requireNonNull(ImageComplianceClientApp.class.getResource("/")).toURI().getPath();
            imagePath = "image/political.png";
            imageUrl = "您的PNG、JPG、JPEG、BMP、GIF、WEBP格式的公网可访问图片链接";
        } catch (URISyntaxException e) {
            logger.error("文件路径加载异常", e);
        }
    }

    public static void main(String[] args) throws Exception {
        ImageComplianceClient correctionClient = new ImageComplianceClient
                .Builder(appId, apiKey, apiSecret)
                .build();

        String pathResp = correctionClient.send(FileUtil.fileToBase64(resourcePath + imagePath), ModeType.BASE64);
        logger.info("图片地址返回结果: {}", pathResp);

        // String urlResp = correctionClient.send(imageUrl, ModeType.LINK);
        // logger.info("图片链接返回结果: {}", urlResp);
    }

}
