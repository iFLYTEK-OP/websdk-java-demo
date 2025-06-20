package cn.xfyun.demo.nlp;

import cn.xfyun.api.TransClient;
import cn.xfyun.config.PropertiesConfig;
import cn.xfyun.model.translate.TransParam;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * @author <ydwang16@iflytek.com>
 * @description 小牛翻译及自研机器翻译
 * @date 2021/6/17
 */
public class TranslateApp {

    private static final Logger logger = LoggerFactory.getLogger(TranslateApp.class);
    private static final String appId = PropertiesConfig.getAppId();
    private static final String apiKey = PropertiesConfig.getApiKey();
    private static final String apiSecret = PropertiesConfig.getApiSecret();

    public static void main(String[] args) {
        TransClient client = new TransClient.Builder(appId, apiKey, apiSecret).build();

        try {
            TransParam param = TransParam.builder()
                    .text("神舟十二号载人飞船发射任务取得圆满成功")
                    .from("cn")
                    .to("en")
                    // 个性化术语ID ( 仅 自研机器翻译（新） 支持)
                    // .resId("您的术语ID")
                    .build();
            // 小牛翻译 (默认中译英)
            // String niuResponse = client.sendNiuTrans("神舟十二号载人飞船发射任务取得圆满成功");
            String niuResponse = client.sendNiuTrans(param);
            logger.info("niuResponse:{}", niuResponse);

            // 自研机器翻译 (默认中译英)
            // String itsResponse = client.sendIst("6月9号是科大讯飞司庆日");
            String itsResponse = client.sendIst(param);
            logger.info("itsResponse:{}", itsResponse);

            // 自研机器翻译（新） (默认中译英)
            // String itsProResponse = client.sendIstV2("6月9号是科大讯飞司庆日");
            String itsProResponse = client.sendIstV2(param);
            logger.info("itsProResponse:{}", itsProResponse);
            JSONObject obj = JSON.parseObject(itsProResponse);
            String text = obj.getJSONObject("payload").getJSONObject("result").getString("text");
            byte[] decodedBytes = Base64.getDecoder().decode(text);
            String decodeRes = new String(decodedBytes, StandardCharsets.UTF_8);
            logger.info("itsPro翻译结果:{}", decodeRes);
        } catch (Exception e) {
            logger.error("翻译失败", e);
        }
    }
}
