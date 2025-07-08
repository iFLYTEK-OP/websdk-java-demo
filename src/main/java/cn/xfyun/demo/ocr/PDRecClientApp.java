package cn.xfyun.demo.ocr;

import cn.xfyun.api.PDRecClient;
import cn.xfyun.config.DocumentEnum;
import cn.xfyun.config.ImgFormat;
import cn.xfyun.config.PropertiesConfig;
import cn.xfyun.exception.BusinessException;
import cn.xfyun.model.document.PDRecParam;
import cn.xfyun.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.SignatureException;
import java.util.Objects;

/**
 * 图片文档还原（Picture Document Reconstruction）
 * 1、APPID、APISecret、APIKey信息获取：<a href="https://console.xfyun.cn/services/document_reduction">...</a>
 * 2、文档地址：<a href="https://www.xfyun.cn/doc/words/picture-document-reconstruction/API.html">...</a>
 */
public class PDRecClientApp {

    private static final Logger logger = LoggerFactory.getLogger(PDRecClientApp.class);
    private static final String appId = PropertiesConfig.getAppId();
    private static final String apiKey = PropertiesConfig.getApiKey();
    private static final String apiSecret = PropertiesConfig.getApiSecret();
    private static final String docImgPath = "image/doc.jpg";
    private static final String dstFilePath = "doc_result.docx";
    private static String resourcePath;

    static {
        try {
            resourcePath = Objects.requireNonNull(PDRecClientApp.class.getResource("/")).toURI().getPath();
        } catch (URISyntaxException e) {
            logger.error("获取资源路径失败", e);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        PDRecClient client = new PDRecClient.Builder()
                .signature(appId, apiKey, apiSecret)
                .build();
        try {
            // 构造入参
            PDRecParam param = PDRecParam.builder()
                    .resultType(DocumentEnum.DOC.getCode())
                    .imgBase64(FileUtil.fileToBase64(resourcePath + docImgPath))
                    .imgFormat(ImgFormat.JPG.getDesc())
                    // 设置dstFile参数 , 则会自动保存文件到此位置
                    .dstFile(new File(resourcePath + dstFilePath))
                    .build();

            // 发送请求
            byte[] send = client.send(param);
            logger.info("生成大小为: {}B的 {}格式文件", send.length, DocumentEnum.getDescByCode(param.getResultType()));
        } catch (IOException e) {
            logger.error("请求失败", e);
        } catch (SignatureException e) {
            logger.error("认证失败", e);
        } catch (BusinessException e) {
            logger.error("业务处理失败", e);
        } finally {
            System.exit(0);
        }
    }
}
