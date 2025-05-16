package cn.xfyun.demo.spark;

import cn.xfyun.api.SparkCustomClient;
import cn.xfyun.config.PropertiesConfig;
import cn.xfyun.model.sparkmodel.FileContent;
import cn.xfyun.model.sparkmodel.FunctionCall;
import cn.xfyun.model.sparkmodel.request.KnowledgeFileUpload;
import cn.xfyun.model.sparkmodel.response.SparkChatResponse;
import cn.xfyun.service.sparkmodel.AbstractSparkModelWebSocketListener;
import cn.xfyun.util.StringUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import okhttp3.Response;
import okhttp3.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.security.SignatureException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.IntStream;

/**
 * （spark-customize）星火自定义大模型
 * 1、APPID、APISecret、APIKey、APIPassword信息获取：<a href="https://console.xfyun.cn/services/custom_api">...</a>
 * 2、文档地址：<a href="https://www.xfyun.cn/doc/spark/OptionalAPI.html">...</a>
 */
public class SparkCustomClientApp {

    private static final Logger logger = LoggerFactory.getLogger(SparkCustomClientApp.class);
    private static final String appId = PropertiesConfig.getAppId();
    private static final String apiKey = PropertiesConfig.getApiKey();
    private static final String apiSecret = PropertiesConfig.getApiSecret();
    private static String filePath;
    private static String resourcePath;

    static {
        try {
            filePath = "document/private.md";
            resourcePath = Objects.requireNonNull(SparkBatchClientApp.class.getResource("/")).toURI().getPath();
        } catch (URISyntaxException e) {
            logger.error("获取资源路径失败", e);
        }
    }

    public static void main(String[] args) throws Exception {
        // 创建知识库
        create("test-1");

        // 上传文件到知识库
        String fileId = upload(new File(resourcePath + filePath), "test-1");

        // 使用知识库文件进行大模型问答(可使用函数调用)
        sparkChatWs(getMessages(fileId), null);
    }

    private static void sparkChatWs(List<FileContent> messages, List<FunctionCall> functions) throws MalformedURLException, UnsupportedEncodingException, SignatureException, InterruptedException {
        SparkCustomClient client = new SparkCustomClient.Builder()
                .signature(appId, apiKey, apiSecret)
                .build();

        SimpleDateFormat sdf = new SimpleDateFormat("yyy-MM-dd HH:mm:ss.SSS");
        Date dateBegin = new Date();

        StringBuffer finalResult = new StringBuffer();
        client.send(messages, functions, new AbstractSparkModelWebSocketListener() {

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
                                SparkChatResponse.Payload.Choices.Text.FunctionCall call = resp.getPayload().getChoices().getText().get(index).getFunctionCall();
                                if (null != call) {
                                    logger.info("函数{} ==> 名称：{}，函数调用内容：{}", index + 1, call.getName(), call.getArguments());
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
                            logger.info("最终识别结果 ==> {}", finalResult);
                            logger.info("本次识别sid ==> {}", resp.getHeader().getSid());
                            webSocket.close(1000, "");
                        }
                    }
                }
            }

            @Override
            public void onFail(WebSocket webSocket, Throwable t, Response response) {
                webSocket.close(1000, t.getMessage());
                System.exit(0);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, @Nullable Response response) {
                webSocket.close(1000, t.getMessage());
                System.exit(0);
            }

            @Override
            public void onClose(WebSocket webSocket, int code, String reason) {
                System.exit(0);
            }
        });
    }

    private static String upload(File file, String knowledgeName) throws IOException {
        SparkCustomClient client = new SparkCustomClient.Builder()
                .signature(appId, apiKey, apiSecret)
                .build();

        KnowledgeFileUpload upload = KnowledgeFileUpload.builder()
                .file(file)
                .knowledgeName(knowledgeName)
                .build();
        String result = client.upload(upload);
        logger.info("上传文件到知识库返回结果 ==> {}", result);
        //获取文件ID
        return JSON.parseObject(result).getJSONObject("result").getString("file_id");
    }

    private static void create(String knowledgeName) throws IOException {
        SparkCustomClient client = new SparkCustomClient.Builder()
                .signature(appId, apiKey, apiSecret)
                .build();

        String result = client.create(knowledgeName);
        logger.info("创建知识库接口返回结果 ==> {}", result);
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

    private static List<FileContent> getMessages(String... fileId) {
        // 多轮交互需要将之前的交互历史按照system->user->assistant->user->assistant规则进行拼接
        List<FileContent> messages = new ArrayList<>();

        // 会话记录
        FileContent fileContent1 = new FileContent();
        fileContent1.setRole("system");
        fileContent1.setContent("你是一个聊天的人工智能助手，可以和人类进行对话。");

        FileContent fileContent2 = new FileContent();
        fileContent2.setRole("user");
        fileContent2.setContent("你好");

        FileContent fileContent3 = new FileContent();
        fileContent3.setRole("assistant");
        fileContent3.setContent("你好！");

        FileContent fileContent4 = new FileContent();
        fileContent4.setRole("user");

        List<FileContent.Content> content = new ArrayList<>();
        FileContent.Content content1 = new FileContent.Content();
        content1.setType("file");
        content1.setFile(Arrays.asList(fileId));
        content.add(content1);

        FileContent.Content content2 = new FileContent.Content();
        content2.setType("text");
        content2.setText("帮我总结一下这一篇文章讲的什么");
        content.add(content2);

        fileContent4.setRole("user");
        fileContent4.setContent(content);

        // messages.add(roleContent1);
        // messages.add(roleContent2);
        // messages.add(roleContent3);
        messages.add(fileContent4);
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
