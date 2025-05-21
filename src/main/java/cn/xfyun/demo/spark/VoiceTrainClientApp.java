package cn.xfyun.demo.spark;

import cn.xfyun.api.VoiceTrainClient;
import cn.xfyun.config.AgeGroupEnum;
import cn.xfyun.config.PropertiesConfig;
import cn.xfyun.config.SexEnum;
import cn.xfyun.model.voiceclone.request.AudioAddParam;
import cn.xfyun.model.voiceclone.request.CreateTaskParam;
import cn.xfyun.util.StringUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * （voice-train）一句话训练
 * 1、APPID、APISecret、APIKey信息获取：<a href="https://console.xfyun.cn/services/oneSentence">...</a>
 * 2、文档地址：<a href="https://www.xfyun.cn/doc/spark/reproduction.html">...</a>
 */
public class VoiceTrainClientApp {

    private static final Logger logger = LoggerFactory.getLogger(VoiceTrainClientApp.class);
    private static final String APP_ID = PropertiesConfig.getAppId();
    private static final String API_KEY = PropertiesConfig.getApiKey();
    private static String filePath;
    private static String resourcePath;

    static {
        try {
            filePath = "audio/cn/train.mp3";
            resourcePath = Objects.requireNonNull(SparkIatZhClientApp.class.getResource("/")).toURI().getPath();
        } catch (URISyntaxException e) {
            logger.error("获取资源路径失败", e);
        }
    }

    public static void main(String[] args) {
        try {
            VoiceTrainClient client = new VoiceTrainClient.Builder(APP_ID, API_KEY).build();
            logger.info("token：{}, 到期时间：{}", client.getToken(), client.getTokenExpiryTime());

            // 获取到训练文本
            String trainTextTree = client.trainText(5001L);
            logger.info("获取到训练文本列表：{}", trainTextTree);

            // 创建任务
            CreateTaskParam createTaskParam = CreateTaskParam.builder()
                    .taskName("task-01")
                    .sex(SexEnum.FEMALE.getValue())
                    .ageGroup(AgeGroupEnum.YOUTH.getValue())
                    .thirdUser("")
                    .language("cn")
                    .resourceName("中文女发音人")
                    // .mosRatio(2.5f)
                    // .denoiseSwitch(1)
                    .build();
            String taskResp = client.createTask(createTaskParam);
            JsonObject taskObj = StringUtils.gson.fromJson(taskResp, JsonObject.class);
            String taskId = taskObj.get("data").getAsString();
            logger.info("创建任务：{}，返回taskId：{}", taskResp, taskId);

            // 添加链接音频
            // AudioAddParam audioAddParam1 = AudioAddParam.builder()
            //         .audioUrl("https开头,wav|mp3|m4a|pcm文件结尾的URL地址")
            //         .taskId(taskId)
            //         .textId(5001L)
            //         .textSegId(1L)
            //         .build();
            // String audioResp = client.audioAdd(audioAddParam1);
            // logger.info("添加链接音频：{}", audioResp);

            // 提交任务
            // String submit = client.submit(taskId);
            // logger.info("提交任务：{}", submit);

            // 提交文件任务(不需要单独调用submit接口)
            AudioAddParam audioAddParam2 = AudioAddParam.builder()
                    .file(new File(resourcePath + filePath))
                    .taskId(taskId)
                    .textId(5001L)
                    .textSegId(1L)
                    .build();
            String submitWithAudio = client.submitWithAudio(audioAddParam2);
            logger.info("提交任务：{}", submitWithAudio);

            while (true) {
                // 根据taskId查询任务结果
                String result = client.result(taskId);
                JSONObject queryObj = JSON.parseObject(result);
                Integer taskStatus = queryObj.getJSONObject("data").getInteger("trainStatus");
                if (Objects.equals(taskStatus, -1)) {
                    logger.info("一句话复刻训练中...");
                }
                if (Objects.equals(taskStatus, 0)) {
                    String message = queryObj.getJSONObject("data").getString("failedDesc");
                    logger.info("一句话复刻训练失败: {}", message);
                    break;
                }
                if (Objects.equals(taskStatus, 2)) {
                    logger.info("一句话复刻训练任务未提交: {}", result);
                    break;
                }
                if (Objects.equals(taskStatus, 1)) {
                    String string = queryObj.getJSONObject("data").getString("assetId");
                    logger.info("一句话复刻训练完成, 声纹ID: {}", string);
                    break;
                }
                TimeUnit.MILLISECONDS.sleep(3000);
            }
        } catch (Exception e) {
            logger.error("请求失败", e);
        }
    }
}
