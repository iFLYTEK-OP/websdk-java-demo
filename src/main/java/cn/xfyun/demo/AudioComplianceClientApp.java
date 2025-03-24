package cn.xfyun.demo;

import cn.xfyun.api.AudioComplianceClient;
import cn.xfyun.config.PropertiesConfig;
import cn.xfyun.model.Audio;
import cn.xfyun.util.StringUtils;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * （audio-compliance）音频合规
 * 1、APPID、APISecret、APIKey信息获取：https://console.xfyun.cn/services/audio_audit
 * 2、文档地址：https://www.xfyun.cn/doc/nlp/AudioModeration/API.html
 */
public class AudioComplianceClientApp {

    private static final Logger logger = LoggerFactory.getLogger(AudioComplianceClientApp.class);

    private static final String appId = PropertiesConfig.getAppId();
    private static final String apiKey = PropertiesConfig.getApiKey();
    private static final String apiSecret = PropertiesConfig.getApiSecret();

    //	private String audioUrl = "https://xfyun-doc.cn-bj.ufileos.com/static%2F16793792882352753%2F1.mp3";// 音频公网地址
    private static final String audioUrl1 = "https://ah3p.bmwwx.cn/bXWjvY";// 音频公网地址
    private static final String audioUrl2 = "https://chuanmei-m-test-integ-env.iflyrec.com/SpeechSynthesisService/5992f214-f43a-433f-a7ec-067ae189513a/133c3269-c823-4499-94ad-e4283167402f.wav";// 音频公网地址
    private static final String audioUrl3 = "https://chuanmei-m-test-integ-env.iflyrec.com/SpeechSynthesisService/5992f214-f43a-433f-a7ec-067ae189513a/133c3269-c823-4499-94ad-e4283167402f.wav";// 音频公网地址

    private static List<String> audios;

    static {
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
                        .audioType("wav")
                        .fileUrl(audioUrl)
                        .name("133c3269-c823-4499-94ad-e4283167402f.wav")
                        .build();
                audioList.add(audio);
            }
        }

        // 发起音频合规任务请求
        String resp = correctionClient.send(audioList);
        logger.info("音频合规调用返回：{}", resp);
        JsonObject obj = StringUtils.gson.fromJson(resp, JsonObject.class);
        String requestId = obj.getAsJsonObject("data").get("request_id").getAsString();
        logger.info("音频合规任务请求Id：{}", requestId);

        // 拿到request_id后主动查询合规结果   如果有回调函数则在完成后自动调用回调接口
        while (true) {
            String query = correctionClient.query("T2025031417270301a5fdb78eca47000");
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
