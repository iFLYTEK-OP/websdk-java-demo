package cn.xfyun.demo.spark;

import cn.hutool.core.util.StrUtil;
import cn.xfyun.api.SparkChatClient;
import cn.xfyun.config.PropertiesConfig;
import cn.xfyun.config.SparkModel;
import cn.xfyun.exception.BusinessException;
import cn.xfyun.model.sparkmodel.FunctionCall;
import cn.xfyun.model.sparkmodel.RoleContent;
import cn.xfyun.model.sparkmodel.SparkChatParam;
import cn.xfyun.model.sparkmodel.WebSearch;
import cn.xfyun.model.sparkmodel.response.SparkChatResponse;
import cn.xfyun.service.sparkmodel.AbstractSparkModelWebSocketListener;
import cn.xfyun.util.StringUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import okhttp3.*;
import okio.BufferedSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.security.SignatureException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.IntStream;

/**
 * （spark-chat）星火大模型
 * 1、APPID、APISecret、APIKey、APIPassword信息获取：<a href="https://console.xfyun.cn/services/bm4">...</a>
 * 2、文档地址：<a href="https://www.xfyun.cn/doc/spark/Web.html">...</a>
 */
public class SparkChatClientApp {

    private static final Logger logger = LoggerFactory.getLogger(SparkChatClientApp.class);
    private static final String appId = PropertiesConfig.getAppId();
    private static final String apiKey = PropertiesConfig.getApiKey();
    private static final String apiSecret = PropertiesConfig.getApiSecret();
    private static final String apiPassword = PropertiesConfig.getSparkApiPassword();

    public static void main(String[] args) throws Exception {
        // 封装入参
        SparkChatParam sendParam = SparkChatParam.builder()
                .messages(getMessages())
                // 增加联网搜索工具  仅Pro、Max、Ultra系列模型支持
                // .webSearch(getWebSearch())
                // 增加函数调用 仅Spark Max/4.0 Ultra 支持了该功能
                // .functions(getFunctions())
                .chatId("123456")
                // .userId("testUse_123")
                .build();

        // 使用websocket方式请求大模型
        sparkChatWs(sendParam);

        // 使用post方式请求大模型
        // sparkChatPost(sendParam);

        // 使用post流式(sse)请求大模型
        // sparkChatStream(sendParam);
    }

    private static void sparkChatWs(SparkChatParam sendParam) throws MalformedURLException, SignatureException {
        SparkChatClient client = new SparkChatClient.Builder()
                .signatureWs(appId, apiKey, apiSecret, SparkModel.SPARK_X1)
                .build();

        SimpleDateFormat sdf = new SimpleDateFormat("yyy-MM-dd HH:mm:ss.SSS");
        Date dateBegin = new Date();

        // 完整回复结果
        StringBuilder finalResult = new StringBuilder();
        // 思维链完整结果
        StringBuilder thinkingResult = new StringBuilder();
        client.send(sendParam, new AbstractSparkModelWebSocketListener() {

            @Override
            public void onSuccess(WebSocket webSocket, SparkChatResponse resp) {
                if (resp.getHeader().getCode() != 0) {
                    logger.error("code=>{}，error=>{}，sid=>{}", resp.getHeader().getCode(), resp.getHeader().getMessage(), resp.getHeader().getSid());
                    logger.warn("错误码查询链接：https://www.xfyun.cn/doc/spark/%E7%B2%BE%E8%B0%83%E6%9C%8D%E5%8A%A1API-websocket.html");
                    System.exit(0);
                    return;
                }

                if (null != resp.getPayload()) {
                    if (null != resp.getPayload().getPlugins()) {
                        List<SparkChatResponse.Payload.Plugin.Text> plugins = resp.getPayload().getPlugins().getText();
                        if (null != plugins && !plugins.isEmpty()) {
                            logger.info("本次会话使用了插件，数量：{}", plugins.size());
                            IntStream.range(0, plugins.size()).forEach(index -> {
                                SparkChatResponse.Payload.Plugin.Text plugin = plugins.get(index);
                                logger.info("插件{} ==> 类型：{}，插件内容：{}", index + 1, plugin.getName(), plugin.getContent());
                            });
                        }
                    }
                    if (null != resp.getPayload().getChoices()) {
                        List<SparkChatResponse.Payload.Choices.Text> text = resp.getPayload().getChoices().getText();
                        // 是否进行了函数调用
                        if (null != text && !text.isEmpty()) {
                            IntStream.range(0, text.size()).forEach(index -> {
                                String content = resp.getPayload().getChoices().getText().get(index).getContent();
                                String reasoningContent = resp.getPayload().getChoices().getText().get(index).getReasoningContent();
                                SparkChatResponse.Payload.Choices.Text.FunctionCall call = resp.getPayload().getChoices().getText().get(index).getFunctionCall();
                                if (null != call) {
                                    logger.info("函数{} ==> 名称：{}，函数调用内容：{}", index + 1, call.getName(), call.getArguments());
                                }
                                if (!StringUtils.isNullOrEmpty(reasoningContent)) {
                                    thinkingResult.append(reasoningContent);
                                    logger.info("思维链结果 ==> {}", reasoningContent);
                                }
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
                            logger.info("最终思维链结果 ==> {}", thinkingResult);
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
            }

            @Override
            public void onClose(WebSocket webSocket, int code, String reason) {
                System.exit(0);
            }
        });
    }

    private static void sparkChatPost(SparkChatParam sendParam) throws IOException {
        SparkChatClient client = new SparkChatClient.Builder()
                .signatureHttp(apiPassword, SparkModel.SPARK_X1)
                .build();

        SimpleDateFormat sdf = new SimpleDateFormat("yyy-MM-dd HH:mm:ss.SSS");
        Date dateBegin = new Date();

        String send = client.send(sendParam);
        logger.debug("请求结果 ==> {}", send);

        JSONObject obj = JSON.parseObject(send);
        JSONArray choices = obj.getJSONArray("choices");
        choices.forEach(message -> {
            JSONObject messageObj = (JSONObject) message;
            String content = messageObj.getJSONObject("message").getString("content");
            String reasoningContent = messageObj.getJSONObject("message").getString("reasoning_content");
            if (StrUtil.isNotEmpty(reasoningContent)) {
                logger.info("大模型思维链结果 ==> {}", reasoningContent);
            }
            if (StrUtil.isNotEmpty(content)) {
                logger.info("大模型回复结果 ==> {}", content);
            }
            String role = messageObj.getJSONObject("message").getString("role");
            if (StrUtil.isNotEmpty(role)) {
                if (role.equals("tool")) {
                    JSONArray toolCalls = messageObj.getJSONObject("message").getJSONArray("tool_calls");
                    if (null != toolCalls) {
                        logger.info("大模型工具调用调用返回结果 ==> {}", toolCalls);
                    }
                } else {
                    if (client.getToolCallsSwitch()) {
                        JSONArray toolCalls = messageObj.getJSONObject("message").getJSONArray("tool_calls");
                        if (null != toolCalls) {
                            logger.info("大模型函数调用调用返回结果 ==> {}", toolCalls);
                        }
                    } else {
                        JSONObject toolCalls = messageObj.getJSONObject("message").getJSONObject("tool_calls");
                        if (null != toolCalls) {
                            logger.info("大模型函数调用调用返回结果 ==> {}", toolCalls);
                        }
                    }
                }
            }
        });

        logger.info("session end");
        Date dateEnd = new Date();
        logger.info("{}开始", sdf.format(dateBegin));
        logger.info("{}结束", sdf.format(dateEnd));
        logger.info("耗时：{}ms", dateEnd.getTime() - dateBegin.getTime());
    }

    private static void sparkChatStream(SparkChatParam sendParam) {
        SparkChatClient client = new SparkChatClient.Builder()
                .signatureHttp(apiPassword, SparkModel.SPARK_X1)
                .build();

        // 统计耗时
        SimpleDateFormat sdf = new SimpleDateFormat("yyy-MM-dd HH:mm:ss.SSS");
        Date dateBegin = new Date();

        // 最终回复结果
        StringBuilder finalResult = new StringBuilder();
        // 思维链结果
        StringBuilder thinkingResult = new StringBuilder();

        client.send(sendParam, new Callback() {
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
                                // logger.info("{}", line);
                                if (line.contains("[DONE]")) {
                                    // 说明数据全部返回完毕，可以关闭连接，释放资源
                                    logger.info("session end");
                                    Date dateEnd = new Date();
                                    logger.info("{}开始", sdf.format(dateBegin));
                                    logger.info("{}结束", sdf.format(dateEnd));
                                    logger.info("耗时：{}ms", dateEnd.getTime() - dateBegin.getTime());
                                    logger.info("最终识别结果 ==> {}", finalResult);
                                    System.exit(0);
                                }
                                // 去掉前缀 "data: "
                                String data = line.substring(5).trim();
                                extractContent(data, finalResult, thinkingResult);
                            }
                        }
                    } catch (IOException e) {
                        logger.error("读取sse返回内容发生异常", e);
                    }
                }
            }
        });
    }

    /**
     * @param data        sse返回的数据
     * @param finalResult 实时回复内容
     */
    private static void extractContent(String data, StringBuilder finalResult, StringBuilder thinkingResult) {
        logger.debug("sse返回数据 ==> {}", data);
        try {
            JSONObject dataObj = JSON.parseObject(data);
            JSONArray choices = dataObj.getJSONArray("choices");
            Integer code = dataObj.getInteger("code");
            if (code != 0) {
                String message = dataObj.getString("message");
                String sid = dataObj.getString("sid");
                logger.error("code=>{}，error=>{}，sid=>{}", code, message, sid);
                logger.warn("错误码查询链接：https://www.xfyun.cn/doc/spark/%E7%B2%BE%E8%B0%83%E6%9C%8D%E5%8A%A1API-websocket.html");
                System.exit(0);
                return;
            }
            choices.forEach(message -> {
                JSONObject messageObj = (JSONObject) message;
                String content = messageObj.getJSONObject("delta").getString("content");
                String reasoningContent = messageObj.getJSONObject("delta").getString("reasoning_content");
                String suggest = Optional.ofNullable(messageObj.getJSONObject("message"))
                        .map(message1 -> message1.getJSONObject("security_suggest"))
                        .map(securitySuggest -> "(" + securitySuggest.getString("action") + ")")
                        .orElse("");
                if (StrUtil.isNotEmpty(reasoningContent)) {
                    logger.info("思维链结果{} ==> {}", suggest, reasoningContent);
                    thinkingResult.append(reasoningContent);
                    return;
                }
                if (StrUtil.isNotEmpty(content)) {
                    logger.info("中间结果 ==> {}", content);
                    finalResult.append(content);
                    return;
                }
                String role = messageObj.getJSONObject("delta").getString("role");
                if (StrUtil.isNotEmpty(role)) {
                    if (role.equals("tool")) {
                        JSONArray toolCalls = messageObj.getJSONObject("delta").getJSONArray("tool_calls");
                        if (null != toolCalls) {
                            logger.info("大模型工具调用调用返回结果 ==> {}", toolCalls);
                        }
                    } else {
                        JSONObject toolCalls = messageObj.getJSONObject("delta").getJSONObject("tool_calls");
                        if (null != toolCalls) {
                            logger.info("大模型函数调用调用返回结果 ==> {}", toolCalls);
                        }
                    }
                }
            });
        } catch (BusinessException bx) {
            throw bx;
        } catch (Exception e) {
            logger.error("解析sse返回内容发生异常", e);
            logger.error("异常数据 ==> {}", data);
        }
    }

    private static List<FunctionCall> getFunctions() {
        List<FunctionCall> functions = new ArrayList<>();
        FunctionCall function = new FunctionCall();
        function.setName("天气查询");
        function.setDescription("天气插件可以提供天气相关信息。你可以提供指定的地点信息、指定的时间点或者时间段信息，来精准检索到天气信息。");
        function.setParameters(getParameters());
        functions.add(function);
        return functions;
    }

    private static WebSearch getWebSearch() {
        WebSearch webSearch = new WebSearch();
        webSearch.setSearchMode("deep");
        webSearch.setShowRefLabel(Boolean.TRUE);
        webSearch.setEnable(Boolean.TRUE);
        return webSearch;
    }

    private static List<RoleContent> getMessages() {
        // 多轮交互需要将之前的交互历史按照system->user->assistant->user->assistant规则进行拼接
        List<RoleContent> messages = new ArrayList<>();

        // 会话记录
        RoleContent roleContent1 = new RoleContent();
        roleContent1.setRole("system");
        roleContent1.setContent("你是一个聊天的人工智能助手，可以和人类进行对话。");
        RoleContent roleContent2 = new RoleContent();
        roleContent2.setRole("user");
        roleContent2.setContent("你好");
        RoleContent roleContent3 = new RoleContent();
        roleContent3.setRole("assistant");
        roleContent3.setContent("你好！");
        // 当前会话
        RoleContent roleContent4 = new RoleContent();
        roleContent4.setRole("user");
        roleContent4.setContent("北京今天天气怎么样");
        // roleContent4.setContent("你如何看待中美关税战?");
        // roleContent4.setContent("特朗普在中美贸易战中扮演的角色");
        // roleContent4.setContent("吴艳妮最新消息");
        // roleContent4.setContent("光刻机的原理是什么");
        // roleContent4.setContent("Stell dich vor");
        // roleContent4.setContent("今日の天気はどうですか。");
        // roleContent4.setContent("오늘 날씨 어때요?");
        // roleContent4.setContent("Какая сегодня погода?");
        // roleContent4.setContent("ما هو الطقس اليوم ؟");
        // roleContent4.setContent("Quelle est la météo aujourd'hui");
        // roleContent4.setContent("¿¿ cómo está el clima hoy?");
        // roleContent4.setContent("Como está o tempo hoje?");

        // messages.add(roleContent1);
        // messages.add(roleContent2);
        // messages.add(roleContent3);
        messages.add(roleContent4);
        return messages;
    }

    private static FunctionCall.Parameters getParameters() {
        FunctionCall.Parameters parameters = new FunctionCall.Parameters();
        parameters.setType("object");
        // 函数的字段
        JSONObject properties = new JSONObject();
        // 字段1 地点
        FunctionCall.Parameters.Field location = new FunctionCall.Parameters.Field();
        location.setType("string");
        location.setDescription("地点，比如北京。");
        // 字段2 日期
        FunctionCall.Parameters.Field date = new FunctionCall.Parameters.Field();
        date.setType("string");
        date.setDescription("日期。");
        // 放到properties转换成字符串存储
        properties.put("location", location);
        properties.put("date", date);
        parameters.setProperties(properties.toJSONString());
        // 必须要返回的字段(示例: 返回地点)
        parameters.setRequired(Collections.singletonList("location"));
        return parameters;
    }
}
