package cn.xfyun.demo;

import cn.xfyun.api.TransClient;
import cn.xfyun.config.PropertiesConfig;
import com.alibaba.fastjson.JSONObject;
/**
 * @author <ydwang16@iflytek.com>
 * @description 小牛翻译及自研机器翻译
 * @date 2021/6/17
 */
public class TranslateApp {

    private static final String appId = PropertiesConfig.getAppId();
    private static final String apiKey = PropertiesConfig.getApiKey();
    private static final String apiSecret = PropertiesConfig.getApiSecret();

    public static void main(String[] args) {
        TransClient client = new TransClient.Builder(appId, apiKey, apiSecret).build();

        try {
            // 小牛翻译
            String niuResponse = client.sendNiuTrans("神舟十二号载人飞船发射任务取得圆满成功");
            System.out.println(JSONObject.toJSONString(niuResponse));

            // 自研机器翻译
            String itsResponse = client.sendIst("6月9号是科大讯飞司庆日");
            System.out.println(JSONObject.toJSONString(itsResponse));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
