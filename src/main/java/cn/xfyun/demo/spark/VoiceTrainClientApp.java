
package cn.xfyun.demo.spark;

import cn.xfyun.api.VoiceTrainClient;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * 一句话训练Demo
 *
 * @author zyding6
 */
public class VoiceTrainClientApp {

    private static final Logger logger = LoggerFactory.getLogger(VoiceTrainClientApp.class);
    private static final String APP_ID = "替换成你的appid";
    private static final String API_KEY = "替换成你的apiKey";

    public static void main(String[] args) {
        try {
            VoiceTrainClient client = new VoiceTrainClient.Builder(APP_ID, API_KEY).build();
            String tokenResp = client.token();
            JSONObject tokenObj = JSONObject.parseObject(tokenResp);
            String token = tokenObj.getString("accesstoken");
            logger.info("获取到token：{}", token);

            String trainTextTree = client.trainText(token, 5001L);
            logger.info("获取到训练文本列表：{}", trainTextTree);

            String taskResp = client.createTask("2025-03-11测试", 2, 2, "百度翻译", "en", "百度翻译女发音人", null, token);
            JSONObject taskObj = JSONObject.parseObject(taskResp);
            String taskId = taskObj.getString("data");
            logger.info("创建任务：{}，返回taskId：{}", taskResp, taskId);

            String audioResp = client.audioAdd(taskId, "https开头,wav|mp3|m4a|pcm文件结尾的URL地址",
                    5001L, 1L, token);
            logger.info("添加链接音频：{}", audioResp);

            String submit = client.submit(taskId, token);
            logger.info("提交任务：{}", submit);

            String result = client.result(taskId, token);
            logger.info("任务结果：{}", result);

            String submitWithAudio = client.submitWithAudio(new File("wav/mp3/m4a/pcm文件地址"), taskId, "5001", "1", token);
            logger.info("提交任务：{}", submitWithAudio);
        } catch (Exception e) {
            logger.error("请求失败", e);
        }
    }
}
