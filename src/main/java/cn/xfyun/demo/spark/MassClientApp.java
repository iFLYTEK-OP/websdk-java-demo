package cn.xfyun.demo.spark;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.xfyun.api.MassClient;
import cn.xfyun.config.PropertiesConfig;
import cn.xfyun.exception.BusinessException;
import cn.xfyun.model.RoleContent;
import cn.xfyun.model.finetuning.response.MassResponse;
import cn.xfyun.service.finetuning.AbstractMassWebSocketListener;
import cn.xfyun.util.StringUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import okhttp3.*;
import okio.BufferedSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.security.SignatureException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * （fine-tuning-text）精练大模型文本对话
 * 1、APPID、APISecret、APIKey、APIPassword信息获取：<a href="https://training.xfyun.cn/model/add">...</a>
 * 2、文档地址：<a href="https://www.xfyun.cn/doc/spark/%E7%B2%BE%E8%B0%83%E6%9C%8D%E5%8A%A1API-websocket.html">...</a>
 */
public class MassClientApp {

    private static final Logger logger = LoggerFactory.getLogger(MassClientApp.class);
    private static final String appId = PropertiesConfig.getAppId();
    private static final String apiKey = PropertiesConfig.getApiKey();
    private static final String apiSecret = PropertiesConfig.getApiSecret();
    private static final String apiPassword = "你的apiPassword";

    public static void main(String[] args) throws Exception {

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

        // ws请求方式
        chatWs(messages);

        // post方式
        // chatPost(messages);

        // 流式请求
        // chatStream(messages);
    }

    private static void chatStream(List<RoleContent> messages) {
        MassClient client = new MassClient.Builder()
                .signatureHttp("0", "xdeepseekr1", apiPassword)
                .requestUrl("https://maas-api.cn-huabei-1.xf-yun.com/v1/chat/completions")
                .build();

        // 统计耗时
        SimpleDateFormat sdf = new SimpleDateFormat("yyy-MM-dd HH:mm:ss.SSS");
        Date dateBegin = new Date();

        // 最终回复结果
        StringBuilder finalResult = new StringBuilder();
        // 最终思维链结果
        StringBuilder thingkingResult = new StringBuilder();

        client.sendStream(messages, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.error("sse连接失败：{}", e.getMessage());
                System.exit(0);
            }

            @Override
            public void onResponse(Call call, Response response) {
                if (!response.isSuccessful()) {
                    logger.error("请求失败，状态码：{}，原因：{}", response.code(), response.message());
                    System.exit(0);
                    return;
                }
                ResponseBody body = response.body();
                if (body != null) {
                    BufferedSource source = body.source();
                    try {
                        while (true) {
                            String line = source.readUtf8Line();
                            if (line == null) {
                                break;
                            }
                            if (line.startsWith("data:")) {
                                // 去掉前缀 "data: "
                                String data = line.substring(5).trim();
                                if (extractContent(data, finalResult, thingkingResult)) {
                                    // 说明数据全部返回完毕，可以关闭连接，释放资源
                                    logger.info("session end");
                                    Date dateEnd = new Date();
                                    logger.info("{}开始", sdf.format(dateBegin));
                                    logger.info("{}结束", sdf.format(dateEnd));
                                    logger.info("耗时：{}ms", dateEnd.getTime() - dateBegin.getTime());
                                    logger.info("完整思维链结果 ==> {}", thingkingResult);
                                    logger.info("最终识别结果 ==> {}", finalResult);
                                    System.exit(0);
                                }
                            }
                        }
                    } catch (IOException e) {
                        logger.error("读取sse返回内容发生异常", e);
                    }
                }
            }
        });
    }

    private static void chatPost(List<RoleContent> messages) throws IOException {
        MassClient client = new MassClient.Builder()
                .signatureHttp("0", "xdeepseekr1", apiPassword)
                .requestUrl("https://maas-api.cn-huabei-1.xf-yun.com/v1/chat/completions")
                .build();

        String result = client.sendPost(messages);
        logger.debug("{} 模型返回结果 ==>{}", client.getDomain(), result);
        JSONObject obj = JSON.parseObject(result);
        String content = obj.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
        logger.info("{} 大模型回复内容 ==>{}", client.getDomain(), content);
    }

    private static void chatWs(List<RoleContent> messages) throws MalformedURLException, UnsupportedEncodingException, SignatureException {
        MassClient client = new MassClient.Builder()
                .signatureWs("0", "xdeepseekr1", appId, apiKey, apiSecret)
                .wsUrl("wss://maas-api.cn-huabei-1.xf-yun.com/v1.1/chat")
                .build();

        // 统计耗时
        SimpleDateFormat sdf = new SimpleDateFormat("yyy-MM-dd HH:mm:ss.SSS");
        Date dateBegin = new Date();

        // 最终回复结果
        StringBuilder finalResult = new StringBuilder();
        // 最终思维链结果
        StringBuilder thingkingResult = new StringBuilder();

        // ws方式
        client.sendWs(messages, new AbstractMassWebSocketListener() {
            @Override
            public void onSuccess(WebSocket webSocket, MassResponse resp) {
                logger.debug("中间返回json结果 ==>{}", JSONUtil.toJsonStr(resp));
                if (resp.getHeader().getCode() != 0) {
                    logger.error("code=>{}，error=>{}，sid=>{}", resp.getHeader().getCode(), resp.getHeader().getMessage(), resp.getHeader().getSid());
                    logger.warn("错误码查询链接：https://www.xfyun.cn/doc/spark/%E7%B2%BE%E8%B0%83%E6%9C%8D%E5%8A%A1API-websocket.html");
                    System.exit(0);
                    return;
                }

                if (null != resp.getPayload() && null != resp.getPayload().getChoices()) {
                    List<MassResponse.Payload.Choices.Text> text = resp.getPayload().getChoices().getText();
                    if (null != text && !text.isEmpty()) {
                        String content = resp.getPayload().getChoices().getText().get(0).getContent();
                        String reasonContent = resp.getPayload().getChoices().getText().get(0).getReasoningContent();
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
                System.exit(0);
            }

            @Override
            public void onClose(WebSocket webSocket, int code, String reason) {
                System.exit(0);
            }
        });
    }

    /**
     * @param data            sse返回的数据
     * @param finalResult     实时回复内容
     * @param thingkingResult 实时思维链结果
     * @return 是否结束
     */
    private static boolean extractContent(String data, StringBuilder finalResult, StringBuilder thingkingResult) {
        logger.debug("sse返回数据 ==> {}", data);
        try {
            JSONObject obj = JSON.parseObject(data);
            JSONObject choice0 = obj.getJSONArray("choices").getJSONObject(0);
            JSONObject delta = choice0.getJSONObject("delta");
            // 结束原因
            String finishReason = choice0.getString("finish_reason");
            if (StrUtil.isNotEmpty(finishReason)) {
                if (finishReason.equals("stop")) {
                    logger.info("本次识别sid ==> {}", obj.getString("id"));
                    return true;
                }
                throw new BusinessException("异常结束: " + finishReason);
            }
            // 回复
            String content = delta.getString("content");
            if (StrUtil.isNotEmpty(content)) {
                logger.info("中间结果 ==> {}", content);
                finalResult.append(content);
            }
            // 思维链
            String reasonContent = delta.getString("reasoning_content");
            if (StrUtil.isNotEmpty(reasonContent)) {
                logger.info("思维链结果... ==> {}", reasonContent);
                thingkingResult.append(reasonContent);
            }
            // 插件
            String pluginContent = delta.getString("plugins_content");
            if (StrUtil.isNotEmpty(pluginContent)) {
                logger.info("插件信息 ==> {}", pluginContent);
            }
        } catch (BusinessException bx) {
            throw bx;
        } catch (Exception e) {
            logger.error("解析sse返回内容发生异常", e);
            logger.error("异常数据 ==> {}", data);
        }
        return false;
    }
}
