package cn.xfyun.demo.spark;

import cn.xfyun.api.SparkAssistantClient;
import cn.xfyun.config.PropertiesConfig;
import cn.xfyun.model.sparkmodel.RoleContent;
import cn.xfyun.model.sparkmodel.SparkChatParam;
import cn.xfyun.model.sparkmodel.response.SparkChatResponse;
import cn.xfyun.service.sparkmodel.AbstractSparkModelWebSocketListener;
import cn.xfyun.util.StringUtils;
import okhttp3.Response;
import okhttp3.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.security.SignatureException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * （spark-assistant）星火助手
 * 1、APPID、APISecret、APIKey、APIPassword信息获取：<a href="https://agent.xfyun.cn/space/agent">...</a>
 * 2、文档地址：<a href="https://www.xfyun.cn/doc/spark/SparkAssistantAPI.html">...</a>
 */
public class SparkAssistantClientApp {

    private static final Logger logger = LoggerFactory.getLogger(SparkAssistantClientApp.class);
    private static final String appId = PropertiesConfig.getAppId();
    private static final String apiKey = PropertiesConfig.getApiKey();
    private static final String apiSecret = PropertiesConfig.getApiSecret();
    private static final String assistantId = "您的助手ID";

    public static void main(String[] args) throws Exception {
        SparkAssistantClient client = new SparkAssistantClient.Builder()
                .signature(appId, apiKey, apiSecret, assistantId)
                .build();

        // 使用websocket方式请求大模型
        SparkChatParam param = SparkChatParam.builder()
                .messages(getMessages())
                .chatId(UUID.randomUUID().toString().substring(0,10))
                .userId("user_001")
                .build();
        sparkChatWs(param, client);
    }

    private static void sparkChatWs(SparkChatParam param,  SparkAssistantClient client) throws MalformedURLException, SignatureException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyy-MM-dd HH:mm:ss.SSS");
        Date dateBegin = new Date();

        StringBuffer finalResult = new StringBuffer();
        client.send(param, new AbstractSparkModelWebSocketListener() {

            @Override
            public void onSuccess(WebSocket webSocket, SparkChatResponse resp) {
                if (resp.getHeader().getCode() != 0) {
                    logger.error("code=>{}，error=>{}，sid=>{}", resp.getHeader().getCode(), resp.getHeader().getMessage(), resp.getHeader().getSid());
                    logger.warn("错误码查询链接：https://www.xfyun.cn/doc/spark/%E7%B2%BE%E8%B0%83%E6%9C%8D%E5%8A%A1API-websocket.html");
                    // System.exit(0);
                    webSocket.close(1000, "");
                    return;
                }

                if (null != resp.getPayload()) {
                    if (null != resp.getPayload().getChoices()) {
                        List<SparkChatResponse.Payload.Choices.Text> text = resp.getPayload().getChoices().getText();
                        // 是否进行了函数调用
                        if (null != text && !text.isEmpty()) {
                            IntStream.range(0, text.size()).forEach(index -> {
                                String content = resp.getPayload().getChoices().getText().get(index).getContent();
                                if (!StringUtils.isNullOrEmpty(content)) {
                                    finalResult.append(content);
                                    logger.info("中间结果 ==> {}", content);
                                }
                            });
                        }

                        if (resp.getPayload().getChoices().getStatus() == 2) {
                            // 说明数据全部返回完毕，可以关闭连接，释放资源
                            logger.info("session end");
                            Date dateEnd = new Date();
                            logger.info("{}开始", sdf.format(dateBegin));
                            logger.info("{}结束", sdf.format(dateEnd));
                            logger.info("耗时：{}ms", dateEnd.getTime() - dateBegin.getTime());
                            logger.info("最终识别结果 ==> {}", finalResult);
                            logger.info("本次识别sid ==> {}", resp.getHeader().getSid());
                            webSocket.close(1000, "");
                        }
                    }
                }
            }

            @Override
            public void onFail(WebSocket webSocket, Throwable t, Response response) {
                logger.error(t.getMessage(), t);
                webSocket.close(1000, t.getMessage());
                System.exit(0);
            }

            @Override
            public void onClose(WebSocket webSocket, int code, String reason) {
                System.exit(0);
            }
        });
    }

    private static List<RoleContent> getMessages() {
        // 多轮交互需要将之前的交互历史按照user(image)->user->assistant->user->assistant规则进行拼接，保证最后一条是user的当前问题
        List<RoleContent> messages = new ArrayList<>();

        // 会话记录
        RoleContent roleContent1 = new RoleContent();
        roleContent1.setRole("user");
        roleContent1.setContent("开始");

        messages.add(roleContent1);
        return messages;
    }
}
