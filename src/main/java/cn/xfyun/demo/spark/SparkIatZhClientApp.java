package cn.xfyun.demo.spark;

import cn.xfyun.api.SparkIatClient;
import cn.xfyun.config.PropertiesConfig;
import cn.xfyun.config.SparkIatModelEnum;
import cn.xfyun.model.sparkiat.response.SparkIatResponse;
import cn.xfyun.service.sparkiat.AbstractSparkIatWebSocketListener;
import cn.xfyun.util.AudioPlayer;
import cn.xfyun.util.MicrophoneAudioSender;
import cn.xfyun.util.StringUtils;
import okhttp3.Response;
import okhttp3.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.SignatureException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * SPARK_IAT( iFly Spark Auto Transform ) 中文大模型语音听写
 * 1、APPID、APISecret、APIKey信息获取：<a href="https://console.xfyun.cn/services/bmc">...</a>
 * 2、文档地址：<a href="https://www.xfyun.cn/doc/spark/spark_zh_iat.html">...</a>
 */
public class SparkIatZhClientApp {

    private static final Logger logger = LoggerFactory.getLogger(SparkIatZhClientApp.class);
    private static final String appId = PropertiesConfig.getAppId();
    private static final String apiKey = PropertiesConfig.getApiKey();
    private static final String apiSecret = PropertiesConfig.getApiSecret();
    private static final String filePath = "audio/spark_iat_cn_16k_10.pcm";
    private static String resourcePath;

    static {
        try {
            resourcePath = Objects.requireNonNull(SparkIatZhClientApp.class.getResource("/")).toURI().getPath();
        } catch (URISyntaxException e) {
            logger.error("获取资源路径失败", e);
        }
    }

    public static void main(String[] args) throws FileNotFoundException, SignatureException, MalformedURLException, InterruptedException {
        SparkIatClient sparkIatClient = new SparkIatClient.Builder()
                .signature(appId, apiKey, apiSecret, SparkIatModelEnum.ZH_CN_MANDARIN.getCode())
                // 流式实时返回转写结果
                .dwa("wpgs")
                .build();

        // 处理从文件中获取的音频数据
        processAudioFromFile(sparkIatClient);

        // 方式二：处理麦克风输入的音频数据
        // processAudioFromMicrophone(sparkIatClient);
    }

    private static void processAudioFromFile(SparkIatClient sparkIatClient) throws FileNotFoundException, MalformedURLException, SignatureException {
        File file = new File(resourcePath + filePath);

        // 调用sdk方法
        sparkIatClient.send(file, getWebSocketListener());

        // 实时播放音频
        play(file);
    }

    private static void processAudioFromMicrophone(SparkIatClient sparkIatClient) throws MalformedURLException, SignatureException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyy-MM-dd HH:mm:ss.SSS");
        Date dateBegin = new Date();

        // 最终结果
        StringBuffer finalResult = new StringBuffer();

        // 存储流式返回结果的Map sn -> content
        Map<Integer, String> contentMap = new TreeMap<>();
        // 调用sdk方法
        sparkIatClient.start(new AbstractSparkIatWebSocketListener() {

            // 麦克风工具类
            MicrophoneAudioSender sender = null;

            @Override
            public void onSuccess(WebSocket webSocket, SparkIatResponse resp) {
                // logger.debug("{}", JSON.toJSONString(resp));
                if (resp.getHeader().getCode() != 0) {
                    logger.error("code=>{}，error=>{}，sid=>{}", resp.getHeader().getCode(), resp.getHeader().getMessage(), resp.getHeader().getSid());
                    logger.warn("错误码查询链接：https://www.xfyun.cn/document/error-code");
                    return;
                }

                if (resp.getPayload() != null) {
                    // 非流式实时返回结果处理方式
                    /*if (resp.getPayload().getResult() != null) {
                        String tansTxt = resp.getPayload().getResult().getText();
                        if (null != tansTxt) {
                            // 解码转写结果
                            byte[] decodedBytes = Base64.getDecoder().decode(resp.getPayload().getResult().getText());
                            String decodeRes = new String(decodedBytes, StandardCharsets.UTF_8);
                            SparkIatResponse.JsonParseText jsonParseText = StringUtils.gson.fromJson(decodeRes, SparkIatResponse.JsonParseText.class);

                            StringBuilder text = new StringBuilder();
                            for (SparkIatResponse.Ws ws : jsonParseText.getWs()) {
                                List<SparkIatResponse.Cw> cwList = ws.getCw();
                                for (SparkIatResponse.Cw cw : cwList) {
                                    text.append(cw.getW());
                                }
                            }

                            finalResult.append(text);
                            logger.info("中间识别结果 ==>{}", text);
                        }
                    }*/

                    // 流式实时返回结果处理方式
                    if (null != resp.getPayload().getResult().getText()) {
                        byte[] decodedBytes = Base64.getDecoder().decode(resp.getPayload().getResult().getText());
                        String decodeRes = new String(decodedBytes, StandardCharsets.UTF_8);
                        // logger.info("中间识别结果 ==>{}", decodeRes);
                        SparkIatResponse.JsonParseText jsonParseText = StringUtils.gson.fromJson(decodeRes, SparkIatResponse.JsonParseText.class);
                        // 拼接单句ws的内容
                        StringBuilder reqResult = getWsContent(jsonParseText);
                        // 根据pgs参数判断是拼接还是替换
                        if (jsonParseText.getPgs().equals("apd")) {
                            // 直接添加
                            contentMap.put(jsonParseText.getSn(), reqResult.toString());
                            // logger.info("中间识别结果 【{}】 拼接后结果==> {}", reqResult, getLastResult(contentMap));
                            logger.info("{}", getLastResult(contentMap));
                        } else if (jsonParseText.getPgs().equals("rpl")) {
                            List<Integer> rg = jsonParseText.getRg();
                            int startIndex = rg.get(0);
                            int endIndex = rg.get(1);
                            // 替换 rg 范围内的内容
                            for (int i = startIndex; i <= endIndex; i++) {
                                contentMap.remove(i);
                            }
                            contentMap.put(jsonParseText.getSn(), reqResult.toString());
                            // logger.info("中间识别结果 【{}】 替换后结果==> {}", reqResult, getLastResult(contentMap));
                            logger.info("{}", getLastResult(contentMap));
                        }
                    }

                    if (resp.getPayload().getResult().getStatus() == 2) {
                        // resp.data.status ==2 说明数据全部返回完毕，可以关闭连接，释放资源
                        logger.info("session end");
                        Date dateEnd = new Date();
                        logger.info("{}开始", sdf.format(dateBegin));
                        logger.info("{}结束", sdf.format(dateEnd));
                        logger.info("耗时：{}ms", dateEnd.getTime() - dateBegin.getTime());
                        if (!contentMap.isEmpty()) {
                            // 获取最终拼接结果
                            logger.info("最终识别结果 ==>{}", getLastResult(contentMap));
                        } else {
                            logger.info("最终识别结果 ==>{}", finalResult);
                        }
                        logger.info("本次识别sid ==>{}", resp.getHeader().getSid());
                        webSocket.close(1000, "");
                        System.exit(0);
                    }
                }
            }

            @Override
            public void onFail(WebSocket webSocket, Throwable t, Response response) {
                logger.error("异常信息: {}", t.getMessage(), t);
                System.exit(0);
            }

            @Override
            public void onClose(WebSocket webSocket, int code, String reason) {
                logger.info("关闭连接,code是{},reason:{}", code, reason);
                System.exit(0);
            }

            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                logger.info("连接成功");
                sparkIatClient.sendMessage(webSocket, null, 0, null);
                sender = new MicrophoneAudioSender((audioData, length) -> {
                    // 发送给 WebSocket
                    sparkIatClient.sendMessage(webSocket, audioData, 1, null);
                });
                sender.start();
            }
        });
    }

    /**
     * 自定义ws监听类
     */
    private static AbstractSparkIatWebSocketListener getWebSocketListener() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyy-MM-dd HH:mm:ss.SSS");
        Date dateBegin = new Date();

        // 最终结果
        StringBuffer finalResult = new StringBuffer();

        // 存储流式返回结果的Map sn -> content
        Map<Integer, String> contentMap = new TreeMap<>();

        return new AbstractSparkIatWebSocketListener() {
            @Override
            public void onSuccess(WebSocket webSocket, SparkIatResponse resp) {
                // logger.debug("{}", JSON.toJSONString(resp));
                if (resp.getHeader().getCode() != 0) {
                    logger.error("code=>{}，error=>{}，sid=>{}", resp.getHeader().getCode(), resp.getHeader().getMessage(), resp.getHeader().getSid());
                    logger.warn("错误码查询链接：https://www.xfyun.cn/document/error-code");
                    return;
                }

                if (resp.getPayload() != null) {
                    // 非流式实时返回结果处理方式
                    /*if (resp.getPayload().getResult() != null) {
                        String tansTxt = resp.getPayload().getResult().getText();
                        if (null != tansTxt) {
                            // 解码转写结果
                            byte[] decodedBytes = Base64.getDecoder().decode(resp.getPayload().getResult().getText());
                            String decodeRes = new String(decodedBytes, StandardCharsets.UTF_8);
                            SparkIatResponse.JsonParseText jsonParseText = StringUtils.gson.fromJson(decodeRes, SparkIatResponse.JsonParseText.class);

                            StringBuilder text = new StringBuilder();
                            for (SparkIatResponse.Ws ws : jsonParseText.getWs()) {
                                List<SparkIatResponse.Cw> cwList = ws.getCw();
                                for (SparkIatResponse.Cw cw : cwList) {
                                    text.append(cw.getW());
                                }
                            }

                            finalResult.append(text);
                            logger.info("中间识别结果 ==>{}", text);
                        }
                    }*/

                    // 流式实时返回结果处理方式
                    if (null != resp.getPayload().getResult().getText()) {
                        byte[] decodedBytes = Base64.getDecoder().decode(resp.getPayload().getResult().getText());
                        String decodeRes = new String(decodedBytes, StandardCharsets.UTF_8);
                        // logger.info("中间识别结果 ==>{}", decodeRes);
                        SparkIatResponse.JsonParseText jsonParseText = StringUtils.gson.fromJson(decodeRes, SparkIatResponse.JsonParseText.class);
                        // 拼接单句ws的内容
                        StringBuilder reqResult = getWsContent(jsonParseText);
                        // 根据pgs参数判断是拼接还是替换
                        if (jsonParseText.getPgs().equals("apd")) {
                            // 直接添加
                            contentMap.put(jsonParseText.getSn(), reqResult.toString());
                            // logger.info("中间识别结果 【{}】 拼接后结果==> {}", reqResult, getLastResult(contentMap));
                            logger.info("{}", getLastResult(contentMap));
                        } else if (jsonParseText.getPgs().equals("rpl")) {
                            List<Integer> rg = jsonParseText.getRg();
                            int startIndex = rg.get(0);
                            int endIndex = rg.get(1);
                            // 替换 rg 范围内的内容
                            for (int i = startIndex; i <= endIndex; i++) {
                                contentMap.remove(i);
                            }
                            contentMap.put(jsonParseText.getSn(), reqResult.toString());
                            // logger.info("中间识别结果 【{}】 替换后结果==> {}", reqResult, getLastResult(contentMap));
                            logger.info("{}", getLastResult(contentMap));
                        }
                    }

                    if (resp.getPayload().getResult().getStatus() == 2) {
                        // resp.data.status ==2 说明数据全部返回完毕，可以关闭连接，释放资源
                        logger.info("session end");
                        Date dateEnd = new Date();
                        logger.info("{}开始", sdf.format(dateBegin));
                        logger.info("{}结束", sdf.format(dateEnd));
                        logger.info("耗时：{}ms", dateEnd.getTime() - dateBegin.getTime());
                        if (!contentMap.isEmpty()) {
                            // 获取最终拼接结果
                            logger.info("最终识别结果 ==>{}", getLastResult(contentMap));
                        } else {
                            logger.info("最终识别结果 ==>{}", finalResult);
                        }
                        logger.info("本次识别sid ==>{}", resp.getHeader().getSid());
                        webSocket.close(1000, "");
                        System.exit(0);
                    }
                }
            }

            @Override
            public void onFail(WebSocket webSocket, Throwable t, Response response) {
                logger.error("异常信息: {}", t.getMessage(), t);
                System.exit(0);
            }

            @Override
            public void onClose(WebSocket webSocket, int code, String reason) {
                logger.info("关闭连接,code是{},reason:{}", code, reason);
                System.exit(0);
            }
        };
    }

    /**
     * 播放工具
     */
    private static void play(File file) {
        AudioPlayer audioPlayer = new AudioPlayer();
        try {
            audioPlayer.playFile(file);
        } catch (Exception e) {
            logger.error("播放音频异常: {}", e.getMessage(), e);
        } finally {
            audioPlayer.stop();
        }
    }

    /**
     * 获取最终结果
     */
    private static String getLastResult(Map<Integer, String> contentMap) {
        StringBuilder result = new StringBuilder();
        for (String part : contentMap.values()) {
            result.append(part);
        }
        return result.toString();
    }

    /**
     * 拼接中间结果
     */
    private static StringBuilder getWsContent(SparkIatResponse.JsonParseText jsonParseText) {
        StringBuilder reqResult = new StringBuilder();
        List<SparkIatResponse.Ws> wsList = jsonParseText.getWs();
        for (SparkIatResponse.Ws ws : wsList) {
            List<SparkIatResponse.Cw> cwList = ws.getCw();
            for (SparkIatResponse.Cw cw : cwList) {
                // logger.info(cw.getW());
                reqResult.append(cw.getW());
            }
        }
        return reqResult;
    }
}
