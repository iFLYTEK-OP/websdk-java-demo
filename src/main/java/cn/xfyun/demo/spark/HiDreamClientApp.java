package cn.xfyun.demo.spark;

import cn.xfyun.api.HiDreamClient;
import cn.xfyun.config.PropertiesConfig;
import cn.xfyun.model.image.HiDreamParam;
import cn.xfyun.util.FileUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * （image-generation-hidream）图片生成
 * 1、APPID、APISecret、APIKey信息获取：<a href="https://console.xfyun.cn/services/hidream">...</a>
 * 2、文档地址：<a href="https://www.xfyun.cn/doc/spark/hidream.html">...</a>
 */
public class HiDreamClientApp {

    private static final Logger logger = LoggerFactory.getLogger(HiDreamClientApp.class);
    private static final String appId = PropertiesConfig.getAppId();
    private static final String apiKey = PropertiesConfig.getApiKey();
    private static final String apiSecret = PropertiesConfig.getApiSecret();
    private static List<String> referenceImages;

    static {
        try {
            // 图片基路径
            String resourcePath = Objects.requireNonNull(HiDreamClientApp.class.getResource("/")).toURI().getPath();
            // 参考图片的路径
            String referenceImage1 = "image/hidream_1.jpg";
            // String referenceImage2 = "hidream_2.jpg";
            // 初始化参考图片列表   可以是url 或者 base64文件
            referenceImages = new ArrayList<>();
            referenceImages.add(FileUtil.fileToBase64(resourcePath + referenceImage1));
            // referenceImages.add(FileUtil.fileToBase64(resourcePath + referenceImage2));
        } catch (URISyntaxException e) {
            logger.error("获取资源路径失败", e);
        } catch (IOException ex) {
            logger.error("文件读取异常", ex);
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        HiDreamClient client = new HiDreamClient
                .Builder(appId, apiKey, apiSecret)
                .build();

        logger.info("请求地址：{}", client.getHostUrl());
        HiDreamParam param = HiDreamParam.builder()
                .image(referenceImages)
                .prompt("请将此图片改为孙悟空大闹天空")
                .aspectRatio("1:1")
                .imgCount(1)
                .build();
        String sendResult = client.send(param);
        logger.info("请求返回结果：{}", sendResult);

        // 结果获取taskId
        JSONObject obj = JSON.parseObject(sendResult);
        if (null == obj.getJSONObject("header") || obj.getJSONObject("header").getInteger("code") != 0) {
            logger.error("请求失败：{}", sendResult);
            return;
        }
        String taskId = obj.getJSONObject("header").getString("task_id");
        logger.info("hidream任务id：{}", taskId);

        while (true) {
            // 根据taskId查询任务结果
            String searchResult = client.query(taskId);
            JSONObject queryObj = JSON.parseObject(searchResult);
            String taskStatus = queryObj.getJSONObject("header").getString("task_status");
            if (Objects.equals(taskStatus, "1")) {
                logger.info("文生图任务待处理...");
            }
            if (Objects.equals(taskStatus, "2")) {
                logger.info("文生图任务处理中...");
            }
            if (Objects.equals(taskStatus, "3")) {
                logger.info("文生图任务处理完成：");
                logger.info(searchResult);
                String base64 = queryObj.getJSONObject("payload").getJSONObject("result").getString("text");
                byte[] decodedBytes = Base64.getDecoder().decode(base64);
                String decodedStr = new String(decodedBytes, StandardCharsets.UTF_8);
                logger.info("生成的图片解码后信息：{}", decodedStr);
                // 获取解码后的图片路径(demo只展示生成一张图片的情况)
                JSONArray imageInfo = JSON.parseArray(decodedStr);
                String imageUrl = imageInfo.getJSONObject(0).getString("image_wm");
                logger.info("生成的图片Url：{}", imageUrl);
                break;
            }
            if (Objects.equals(taskStatus, "4")) {
                logger.info("文生图任务回调完成：");
                logger.info(searchResult);
                break;
            }
            TimeUnit.MILLISECONDS.sleep(3000);
        }
    }
}
