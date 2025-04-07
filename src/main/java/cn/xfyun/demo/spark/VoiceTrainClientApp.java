package cn.xfyun.demo.spark;

import cn.xfyun.api.VoiceTrainClient;
import cn.xfyun.model.voiceclone.request.AudioAddParam;
import cn.xfyun.model.voiceclone.request.CreateTaskParam;
import cn.xfyun.util.StringUtils;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * （voice-train）一句话训练
 * 1、APPID、APISecret、APIKey信息获取：<a href="https://console.xfyun.cn/services/oneSentence">...</a>
 * 2、文档地址：<a href="https://www.xfyun.cn/doc/spark/reproduction.html">...</a>
 */
public class VoiceTrainClientApp {

    private static final Logger logger = LoggerFactory.getLogger(VoiceTrainClientApp.class);
    private static final String APP_ID = "您的APP_ID";
    private static final String API_KEY = "您的API_KEY";

    public static void main(String[] args) {
        try {
            VoiceTrainClient client = new VoiceTrainClient.Builder(APP_ID, API_KEY).build();
            logger.info("token：{}, 到期时间：{}", client.getToken(), client.getTokenExpiryTime());

            // 获取到训练文本
            String trainTextTree = client.trainText(5001L);
            logger.info("获取到训练文本列表：{}", trainTextTree);

            // 创建任务
            CreateTaskParam createTaskParam = CreateTaskParam.builder()
                    .taskName("2025-03-11测试")
                    .sex(2)
                    .ageGroup(2)
                    .thirdUser("百度翻译")
                    .language("en")
                    .resourceName("百度翻译女发音人")
                    .build();
            String taskResp = client.createTask(createTaskParam);
            JsonObject taskObj = StringUtils.gson.fromJson(taskResp, JsonObject.class);
            String taskId = taskObj.get("data").getAsString();
            logger.info("创建任务：{}，返回taskId：{}", taskResp, taskId);

            // 添加链接音频
            AudioAddParam audioAddParam1 = AudioAddParam.builder()
                    .audioUrl("https开头,wav|mp3|m4a|pcm文件结尾的URL地址")
                    .taskId(taskId)
                    .textId(5001L)
                    .textSegId(1L)
                    .build();
            String audioResp = client.audioAdd(audioAddParam1);
            logger.info("添加链接音频：{}", audioResp);

            // 提交任务
            String submit = client.submit(taskId);
            logger.info("提交任务：{}", submit);

            // 提交文件任务
            AudioAddParam audioAddParam2 = AudioAddParam.builder()
                    .file(new File("wav/mp3/m4a/pcm文件地址"))
                    .taskId(taskId)
                    .textId(5001L)
                    .textSegId(1L)
                    .build();
            String submitWithAudio = client.submitWithAudio(audioAddParam2);
            logger.info("提交任务：{}", submitWithAudio);

            // 任务结果
            String result = client.result(taskId);
            logger.info("任务结果：{}", result);
        } catch (Exception e) {
            logger.error("请求失败", e);
        }
    }
}
