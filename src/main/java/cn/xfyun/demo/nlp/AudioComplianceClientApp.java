package cn.xfyun.demo.nlp;

import cn.xfyun.api.AudioComplianceClient;
import cn.xfyun.config.AudioFormat;
import cn.xfyun.config.PropertiesConfig;
import cn.xfyun.model.Audio;
import cn.xfyun.util.StringUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * （audio-compliance）音频合规
 * 1、APPID、APISecret、APIKey信息获取：<a href="https://console.xfyun.cn/services/audio_audit">...</a>
 * 2、文档地址：<a href="https://www.xfyun.cn/doc/nlp/AudioModeration/API.html">...</a>
 */
public class AudioComplianceClientApp {

    private static final Logger logger = LoggerFactory.getLogger(AudioComplianceClientApp.class);
    private static final String appId = PropertiesConfig.getAppId();
    private static final String apiKey = PropertiesConfig.getApiKey();
    private static final String apiSecret = PropertiesConfig.getApiSecret();
    /**
     * 音频公网地址
     */
    private static final List<String> audios;

    static {
        String audioUrl1 = "您的mp3、alaw、ulaw、pcm、aac、wav格式的公网可访问音频文件URL";
        audios = Arrays.asList(audioUrl1);
    }

    public static void main(String[] args) throws Exception {
        AudioComplianceClient correctionClient = new AudioComplianceClient
                .Builder(appId, apiKey, apiSecret)
                .build();

        List<Audio> audioList = new ArrayList<>();
        for (String audioUrl : audios) {
            if (!StringUtils.isNullOrEmpty(audioUrl)) {
                Audio audio = new Audio.Builder()
                        // 文件格式 AudioFormat 枚举, 以下为MP3格式的示例
                        .audioType(AudioFormat.MP3.getFormat())
                        .fileUrl(audioUrl)
                        .name("您的音频名称")
                        .build();
                audioList.add(audio);
            }
        }

        // 发起音频合规任务请求
        String resp = correctionClient.send(audioList);
        logger.info("音频合规调用返回：{}", resp);
        
        JSONObject obj = JSON.parseObject(resp);
        String code = obj.getString("code");
        if (!"000000".equals(code)) {
            logger.error("音频合规调用失败：{}", resp);
            return;
        }
        String requestId = obj.getJSONObject("data").getString("request_id");
        logger.info("音频合规任务请求Id：{}", requestId);

        // 拿到request_id后主动查询合规结果   如果有回调函数则在完成后自动调用回调接口
        while (true) {
            String query = correctionClient.query(requestId);
            JsonObject queryObj = StringUtils.gson.fromJson(query, JsonObject.class);
            int auditStatus = queryObj.getAsJsonObject("data").get("audit_status").getAsInt();
            if (auditStatus == 0) {
                logger.info("音频合规待审核...");
            }
            if (auditStatus == 1) {
                logger.info("音频合规审核中...");
            }
            if (auditStatus == 2) {
                logger.info("音频合规审核完成：");
                logger.info(query);
                break;
            }
            if (auditStatus == 4) {
                logger.info("音频合规审核异常：");
                logger.info(query);
                break;
            }
            TimeUnit.MILLISECONDS.sleep(3000);
        }
    }
}
