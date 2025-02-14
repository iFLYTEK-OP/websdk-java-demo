package cn.xfyun.demo;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import cn.xfyun.api.LfasrClient;
import cn.xfyun.config.LfasrTaskStatusEnum;
import cn.xfyun.config.PropertiesConfig;
import cn.xfyun.model.response.lfasr.LfasrMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.security.SignatureException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 *  ( Long Form ASR ) 语音转写
 1、APPID、SecretKey信息获取：https://console.xfyun.cn/services/lfasr
 2、文档地址：https://www.xfyun.cn/doc/asr/ifasr_new/API.html
 */
public class LfasrClientApp {
    private static final Logger logger = LoggerFactory.getLogger(LfasrClient.class);

    private static final String APP_ID = PropertiesConfig.getAppId();
    private static final String SECRET_KEY = PropertiesConfig.getLfasrSecretKey();
    private static String AUDIO_FILE_PATH;

    static {
        try {
            AUDIO_FILE_PATH = LfasrClientApp.class.getResource("/").toURI().getPath() + "/audio/lfasr.wav";
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws SignatureException, InterruptedException {

        //1、创建客户端实例
        LfasrClient lfasrClient = new LfasrClient.Builder(APP_ID, SECRET_KEY).slice(102400).build();

        //2、上传
        LfasrMessage task = lfasrClient.upload(AUDIO_FILE_PATH);
        String taskId = task.getData();
        logger.info("转写任务 taskId：" + taskId);


        //3、查看转写进度
        int status = 0;
        while (LfasrTaskStatusEnum.STATUS_9.getKey() != status) {
            LfasrMessage message = lfasrClient.getProgress(taskId);

            logger.info(message.toString());
            Gson gson = new Gson();
            Map<String, String> map = gson.fromJson(message.getData(), new TypeToken<Map<String, String>>() {
            }.getType());
            status = Integer.parseInt(map.get("status"));
            TimeUnit.SECONDS.sleep(2);
        }
        //4、获取结果
        LfasrMessage result = lfasrClient.getResult(taskId);
        logger.info("转写结果: \n" + result.getData());
        System.exit(0);
    }
}
