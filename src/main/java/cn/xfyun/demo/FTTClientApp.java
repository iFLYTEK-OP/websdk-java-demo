package cn.xfyun.demo;

import cn.hutool.json.JSONUtil;
import cn.xfyun.api.FTTClient;
import cn.xfyun.config.PropertiesConfig;
import cn.xfyun.model.RoleContent;
import cn.xfyun.model.finetuning.response.FTTResponse;
import cn.xfyun.service.finetuning.AbstractFTTWebSocketListener;
import cn.xfyun.util.StringUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import okhttp3.Response;
import okhttp3.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * （fine-tuning-text）精练大模型文本对话
 * 1、APPID、APISecret、APIKey信息获取：https://training.xfyun.cn/model/add
 * 2、文档地址：https://www.xfyun.cn/doc/spark/%E7%B2%BE%E8%B0%83%E6%9C%8D%E5%8A%A1API-websocket.html
 */
public class FTTClientApp {
    private static final Logger logger = LoggerFactory.getLogger(FTTClientApp.class);
    private static final String appId = PropertiesConfig.getAppId();
    private static final String apiKey = PropertiesConfig.getApiKey();
    private static final String apiSecret = PropertiesConfig.getApiSecret();


    public static void main(String[] args) throws Exception {
        FTTClient client = new FTTClient.Builder()
                .signatureWs("0", "xdeepseekv3", appId, apiKey, apiSecret)
//                .signatureWs("0", "xdeepseekr1", appId, apiKey, apiSecret)
//                .signatureHttp("0", "xdeepseekr1", apiKey)
                .wsUrl("wss://maas-api.cn-huabei-1.xf-yun.com/v1.1/chat")
//                .requestUrl("https://maas-api.cn-huabei-1.xf-yun.com/v1")
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();

        List<RoleContent> messages = new ArrayList<>();
        RoleContent roleContent = new RoleContent();
        roleContent.setRole("user");
        roleContent.setContent("你好");
        RoleContent roleContent1 = new RoleContent();
        roleContent1.setRole("assistant");
        roleContent1.setContent("你好！");
        RoleContent roleContent2 = new RoleContent();
        roleContent2.setRole("user");
        roleContent2.setContent("你是谁");
        RoleContent roleContent3 = new RoleContent();
        roleContent3.setRole("assistant");
        roleContent3.setContent("我是Spark API。");
        RoleContent roleContent4 = new RoleContent();
        roleContent4.setRole("user");
        roleContent4.setContent("帮我讲一个笑话");

        messages.add(roleContent);
        messages.add(roleContent1);
        messages.add(roleContent2);
        messages.add(roleContent3);
        messages.add(roleContent4);

        SimpleDateFormat sdf = new SimpleDateFormat("yyy-MM-dd HH:mm:ss.SSS");
        Date dateBegin = new Date();

        StringBuffer finalResult = new StringBuffer();
        StringBuffer thingkingResult = new StringBuffer();
        client.send(messages, new AbstractFTTWebSocketListener() {
            @Override
            public void onSuccess(WebSocket webSocket, FTTResponse resp) {
                logger.debug("中间返回json结果 ==>{}", JSONUtil.toJsonStr(resp));
                if (resp.getHeader().getCode() != 0) {
                    logger.error("code=>{}，error=>{}，sid=>{}", resp.getHeader().getCode(), resp.getHeader().getMessage(), resp.getHeader().getSid());
                    logger.warn("错误码查询链接：https://www.xfyun.cn/doc/spark/%E7%B2%BE%E8%B0%83%E6%9C%8D%E5%8A%A1API-websocket.html");
                    return;
                }

                if (null != resp.getPayload() && null != resp.getPayload().getChoices()) {
                    List<FTTResponse.Payload.Choices.Text> text = resp.getPayload().getChoices().getText();
                    if (null != text && !text.isEmpty()) {
                        String content = resp.getPayload().getChoices().getText().get(0).getContent();
                        String reasonContent = resp.getPayload().getChoices().getText().get(0).getReasoning_content();
                        if (!StringUtils.isNullOrEmpty(reasonContent)) {
                            thingkingResult.append(reasonContent);
                            logger.info("思维链结果... ==> {}", reasonContent);
                        } else if (!StringUtils.isNullOrEmpty(content)) {
                            finalResult.append(content);
                            logger.info("中间结果 ==> {}", content);
                        }
                    }

                    if (resp.getPayload().getChoices().getStatus() == 2) {
                        // 说明数据全部返回完毕，可以关闭连接，释放资源
                        logger.info("session end");
                        Date dateEnd = new Date();
                        logger.info("{}开始", sdf.format(dateBegin));
                        logger.info("{}结束", sdf.format(dateEnd));
                        logger.info("耗时：{}ms", dateEnd.getTime() - dateBegin.getTime());
                        logger.info("完整思维链结果 ==> {}", thingkingResult);
                        logger.info("最终识别结果 ==> {}", finalResult);
                        logger.info("本次识别sid ==> {}", resp.getHeader().getSid());
                        client.closeWebsocket();
                        System.exit(0);
                    }
                }
            }

            @Override
            public void onFail(WebSocket webSocket, Throwable t, Response response) {
                client.closeWebsocket();
            }
        });

        //post方式
//        String result = client.send(messages);
//        logger.debug("{} 模型返回结果 ==>{}", client.getDomain(), result);
//        JSONObject obj = JSON.parseObject(result);
//        String content = obj.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
//        logger.info("{} 大模型回复内容 ==>{}", client.getDomain(), content);
    }
}
