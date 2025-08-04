package cn.xfyun.demo.spark;

import cn.xfyun.api.AgentClient;
import cn.xfyun.config.PropertiesConfig;
import cn.xfyun.exception.BusinessException;
import cn.xfyun.model.agent.AgentChatParam;
import cn.xfyun.model.agent.AgentResumeParam;
import cn.xfyun.service.agent.AgentCallback;
import cn.xfyun.util.StringUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import okhttp3.Call;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.Scanner;

/**
 * （ai-ppt-v2）智能体工作流
 * 1、APPID、APISecret、APIKey、APIPassword信息获取：<a href="https://www.xfyun.cn/doc/spark/Agent01-%E5%B9%B3%E5%8F%B0%E4%BB%8B%E7%BB%8D.html">...</a>
 * 2、文档地址：<a href="https://www.xfyun.cn/doc/spark/PPTv2.html">...</a>
 */
public class AgentClientApp {

    private static final Logger logger = LoggerFactory.getLogger(AgentClientApp.class);
    private static final String API_KEY = PropertiesConfig.getApiKey();
    private static final String API_SECRET = PropertiesConfig.getApiSecret();
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyy-MM-dd HH:mm:ss.SSS");
    private static String filePath;
    private static String resourcePath;

    static {
        try {
            filePath = "image/hidream_1.jpg";
            resourcePath = Objects.requireNonNull(AIPPTV2ClientApp.class.getResource("/")).toURI().getPath();
        } catch (URISyntaxException e) {
            logger.error("获取资源路径失败", e);
        }
    }

    public static void main(String[] args) {
        try {
            AgentClient client = new AgentClient.Builder(API_KEY, API_SECRET).build();

            JSONObject parameter = new JSONObject();
            parameter.put("AGENT_USER_INPUT", "今天天气怎么样");
            AgentChatParam agentChatParam = AgentChatParam.builder()
                    // .flowId("7351173267335847938")
                    .flowId("7351431612989308928")
                    .parameters(parameter)
                    .build();

            // 流式请求
            stream(client, agentChatParam);

            // 非流式请求
            // generate(client, agentChatParam);

            // 上传文件
            // uploadFile(client, new File(resourcePath + filePath));
        } catch (Exception e) {
            logger.error("请求失败", e);
        }
    }

    private static void uploadFile(AgentClient client, File file) throws IOException {
        String result = client.uploadFile(file);
        logger.info(result);
    }

    private static void generate(AgentClient client, AgentChatParam agentChatParam) throws IOException {
        String result = client.completion(agentChatParam);
        logger.info("工作流返回结果：{}", result);
        JSONObject obj = JSON.parseObject(result);
        int code = obj.getIntValue("code");
        if (code != 0) {
            logger.error(result);
            return;
        }
        JSONObject messages = obj.getJSONArray("choices").getJSONObject(0).getJSONObject("delta");
        logger.info("解析结果: {}", messages);
    }

    private static void stream(AgentClient client, AgentChatParam agentChatParam) {
        Date dateBegin = new Date();
        StringBuilder finalResult = new StringBuilder();
        StringBuilder thingkingResult = new StringBuilder();

        client.completion(agentChatParam, getCallback(finalResult, thingkingResult, client, dateBegin));
    }

    private static AgentCallback getCallback(StringBuilder finalResult, StringBuilder thingkingResult, AgentClient client, Date dateBegin) {
        return new AgentCallback() {
            @Override
            public void onEvent(Call call, String id, String type, String data) {
                resultHandler(data, finalResult, thingkingResult, client, dateBegin);
            }

            @Override
            public void onFail(Call call, Throwable t) {
                logger.error("sse通信出错", t);
            }

            @Override
            public void onClosed(Call call) {
                logger.info("sse断开链接");
                call.cancel();
            }

            @Override
            public void onOpen(Call call, Response response) {
                logger.info("sse建立链接");
            }
        };
    }

    /**
     * @param data            sse返回的数据
     * @param finalResult     实时回复内容
     * @param thingkingResult 实时思维链结果
     */
    private static void resultHandler(String data,
                                          StringBuilder finalResult,
                                          StringBuilder thingkingResult,
                                          AgentClient client,
                                          Date dateBegin) {
        // logger.info("sse返回数据 ==> {}", data);
        try (Scanner scanner = new Scanner(System.in)) {
            JSONObject obj = JSON.parseObject(data);
            JSONObject choice0 = obj.getJSONArray("choices").getJSONObject(0);
            JSONObject delta = choice0.getJSONObject("delta");
            JSONObject eventData = obj.getJSONObject("event_data");
            JSONObject step = obj.getJSONObject("workflow_step");
            // 触发事件
            if (null != eventData) {
                String eventId = eventData.getString("event_id");
                JSONObject value = eventData.getJSONObject("value");
                String type = value.getString("type");
                String content = value.getString("content");
                if ("option".equals(type)) {
                    logger.info(content);
                    JSONArray options = value.getJSONArray("option");
                    for (Object option : options) {
                        JSONObject item = JSON.parseObject(JSON.toJSONString(option));
                        logger.info("{}: {}", item.getString("id"), item.getString("text"));
                    }
                }
                String choice = scanner.nextLine();
                AgentResumeParam param = AgentResumeParam.builder()
                        .eventId(eventId)
                        .eventType("resume")
                        .content(choice)
                        .build();
                client.resume(param, getCallback(finalResult, thingkingResult, client, dateBegin));
            }
            // 回复
            String content = delta.getString("content");
            if (!StringUtils.isNullOrEmpty(content)) {
                logger.info("返回结果 ==> {}", content);
                finalResult.append(content);
            }
            // 思维链
            String reasonContent = delta.getString("reasoning_content");
            if (!StringUtils.isNullOrEmpty(reasonContent)) {
                logger.info("思维链结果... ==> {}", reasonContent);
                thingkingResult.append(reasonContent);
            }
            // 插件
            String pluginContent = delta.getString("plugins_content");
            if (!StringUtils.isNullOrEmpty(pluginContent)) {
                logger.info("插件信息 ==> {}", pluginContent);
            }
            // 结束原因
            String finishReason = choice0.getString("finish_reason");
            if (!StringUtils.isNullOrEmpty(finishReason)) {
                if (finishReason.equals("stop")/* || finishReason.equals("interrupt")*/) {
                    if (null != step) {
                        Integer seq = step.getInteger("seq");
                        Float progress = step.getFloat("progress");
                        logger.info(">>>>>>>>>>>>第{}次返回, 进度{}%>>>>>>>>>>>>\r\n\n", seq + 1, progress*100);
                    }
                    logger.info("本次识别sid ==> {}", obj.getString("id"));
                    // 说明数据全部返回完毕，可以关闭连接，释放资源
                    Date dateEnd = new Date();
                    logger.info("{}开始", sdf.format(dateBegin));
                    logger.info("{}结束", sdf.format(dateEnd));
                    logger.info("耗时：{}ms", dateEnd.getTime() - dateBegin.getTime());
                    if (!StringUtils.isNullOrEmpty(thingkingResult.toString())) {
                        logger.info("完整思维链结果 ==> {}", thingkingResult);
                    }
                    logger.info("最终识别结果 ==> {}", finalResult);
                    return;
                }
            }
            // 进度打印
            if (null != step) {
                Integer seq = step.getInteger("seq");
                Float progress = step.getFloat("progress");
                logger.info(">>>>>>>>>>>>第{}次返回, 进度{}%>>>>>>>>>>>>\r\n\n", seq + 1, progress*100);
            }
        } catch (BusinessException bx) {
            throw bx;
        } catch (Exception e) {
            logger.error("解析sse返回内容发生异常", e);
            logger.info("异常数据 ==> {}", data);
        }
    }
}
