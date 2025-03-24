package cn.xfyun.demo;

import cn.xfyun.api.VideoComplianceClient;
import cn.xfyun.config.PropertiesConfig;
import cn.xfyun.config.VideoFormat;
import cn.xfyun.model.Video;
import cn.xfyun.util.StringUtils;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * （video-compliance）视频合规
 * 1、APPID、APISecret、APIKey信息获取：https://console.xfyun.cn/services/video_audit
 * 2、文档地址：https://www.xfyun.cn/doc/nlp/VideoModeration/API.html
 */
public class VideoComplianceClientApp {
    private static final Logger logger = LoggerFactory.getLogger(VideoComplianceClientApp.class);
    private static final String appId = PropertiesConfig.getAppId();
    private static final String apiKey = PropertiesConfig.getApiKey();
    private static final String apiSecret = PropertiesConfig.getApiSecret();

    // 视频公网地址1
    private static final String videoUrl1 = "https://xfyun-doc.cn-bj.ufileos.com/static/16794773437998668/1.mp4";
    // 视频公网地址2
    private static final String videoUrl2 = "";
    // 视频公网地址3
    private static final String videoUrl3 = "";

    private static List<String> videos;

    static {
        videos = Arrays.asList(videoUrl1);
    }

    public static void main(String[] args) throws Exception {
        VideoComplianceClient correctionClient = new VideoComplianceClient
                .Builder(appId, apiKey, apiSecret)
                .build();

        // 创建视频信息列表
        List<Video> videoList = new ArrayList<>();
        for (String videoUrl : videos) {
            if (!StringUtils.isNullOrEmpty(videoUrl)) {
                Video video = new Video.Builder()
                        .videoType(VideoFormat.MP4.getFormat())
                        .fileUrl(videoUrl)
                        .name("1.mp4")
                        .build();
                videoList.add(video);
            }
        }

        // 发起音频合规任务请求
        String resp = correctionClient.send(videoList);
        logger.info("视频合规调用返回：{}", resp);
        JsonObject obj = StringUtils.gson.fromJson(resp, JsonObject.class);
        String requestId = obj.getAsJsonObject("data").get("request_id").getAsString();
        logger.info("视频合规任务请求Id：{}", requestId);

        // 拿到request_id后主动查询合规结果   如果有回调函数则在完成后自动调用回调接口
        while (true) {
            String query = correctionClient.query("T2025031417270301a5fdb78eca47000");
            JsonObject queryObj = StringUtils.gson.fromJson(query, JsonObject.class);
            int auditStatus = queryObj.getAsJsonObject("data").get("audit_status").getAsInt();
            if (auditStatus == 0) {
                logger.info("视频合规待审核...");
            }
            if (auditStatus == 1) {
                logger.info("视频合规审核中...");
            }
            if (auditStatus == 2) {
                logger.info("视频合规审核完成：");
                logger.info(query);
                break;
            }
            if (auditStatus == 4) {
                logger.info("视频合规审核异常：");
                logger.info(query);
                break;
            }
            TimeUnit.MILLISECONDS.sleep(3000);
        }
    }

}
