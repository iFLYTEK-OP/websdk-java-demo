package cn.xfyun.demo;

import cn.xfyun.api.TransClient;
import cn.xfyun.config.PropertiesConfig;
import cn.xfyun.model.response.trans.TransResponse;
import com.alibaba.fastjson.JSONObject;

import java.io.IOException;
import java.security.SignatureException;
import java.util.Map;

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
        TransClient client = new TransClient.Builder().signature(appId, apiKey, apiSecret).build();

        try {
            // 小牛翻译
            TransResponse niuResponse = client.sendNiuTrans("神舟十二号载人飞船发射任务取得圆满成功"
                    ,"cn","en");
            System.out.println(JSONObject.toJSONString(niuResponse));
            Map<String,String> niuResult = niuResponse.getData().getResult().getTrans_result();
            System.out.println("小牛翻译原文为："+niuResult.get("src"));
            System.out.println("小牛翻译结果为："+niuResult.get("dst"));

            // 自研机器翻译
            TransResponse itsResponse = client.sendIst("6月9号是科大讯飞司庆日","cn","en");
            System.out.println(JSONObject.toJSONString(itsResponse));
            Map<String,String> itsResult = itsResponse.getData().getResult().getTrans_result();
            System.out.println("自研翻译原文为："+itsResult.get("src"));
            System.out.println("自研翻译结果为："+itsResult.get("dst"));

        } catch (SignatureException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
