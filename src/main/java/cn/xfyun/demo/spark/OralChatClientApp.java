package cn.xfyun.demo.spark;

import cn.xfyun.api.OralChatClient;
import cn.xfyun.config.PropertiesConfig;
import cn.xfyun.config.StreamMode;
import cn.xfyun.model.oralchat.OralChatParam;
import cn.xfyun.util.AudioPlayer;
import cn.xfyun.util.MicrophoneAudioSender;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 超拟人交互 API
 * 1、APPID、APISecret、APIKey信息获取：<a href="https://www.xfyun.cn/solutions/sparkos_interactive">...</a>
 * 2、文档地址：<a href="https://www.xfyun.cn/doc/spark/sparkos_interactive.html">...</a>
 */
public class OralChatClientApp {

    private static final Logger logger = LoggerFactory.getLogger(OralChatClientApp.class);
    private static final String appId = PropertiesConfig.getAppId();
    private static final String apiKey = PropertiesConfig.getApiKey();
    private static final String apiSecret = PropertiesConfig.getApiSecret();
    private static final String filePath = "audio/cn/天气16K.wav";
    private static String resourcePath;

    static {
        try {
            resourcePath = Objects.requireNonNull(SparkIatZhClientApp.class.getResource("/")).toURI().getPath();
        } catch (URISyntaxException e) {
            logger.error("获取资源路径失败", e);
        }
    }

    public static void main(String[] args) throws FileNotFoundException, SignatureException, MalformedURLException, InterruptedException {
        OralChatClient oralChatClient = new OralChatClient.Builder()
                .signature(appId, apiKey, apiSecret)
                // 流式实时返回转写结果 (仅中文支持)
                .dwa("wpgs")
                .build();

        // 方式一：处理从文件中获取的音频数据
        processAudioFromFile(oralChatClient);

        // 方式二：处理麦克风输入的音频数据
        // processAudioFromMicrophone();
    }

    private static void processAudioFromFile(OralChatClient oralChatClient) {
        OralChatParam param = OralChatParam.builder()
                .interactMode(StreamMode.CONTINUOUS_VAD.getValue())
                .uid("youtestuid")
                .build();

        // 流式播放器
        AudioPlayer player = new AudioPlayer();
        player.start();

        try (Scanner scanner = new Scanner(System.in)) {
            WebSocket socket = oralChatClient.start(param, getListener(player));

            if (StreamMode.CONTINUOUS_VAD.getValue().equals(param.getInteractMode())) {
                logger.info("超拟人单工交互模式开启...");
                logger.info("输入exit停止交互...");
                sendChat(oralChatClient, socket, param);
                while (true) {
                    logger.info("按回车开启第{}轮会话", param.getStmid().get() + 2);
                    String s = scanner.nextLine();
                    if ("exit".equalsIgnoreCase(s)) {
                        break;
                    }
                    sendChat(oralChatClient, socket, param);
                }
            } else {
                logger.info("超拟人双工交互模式开启...");
                logger.info("正在聆听，按回车结束...");
                for (int i = 0; i < 10; i++) {
                    logger.info("第{}论对话", i + 1);
                    sendChat(oralChatClient, socket, param);
                    TimeUnit.MILLISECONDS.sleep(15000);
                }
                scanner.nextLine();
            }

            oralChatClient.stop(socket);
            TimeUnit.MILLISECONDS.sleep(3000);
        } catch (SignatureException | MalformedURLException e) {
            logger.error("API签名验证失败", e);
            throw new RuntimeException("服务鉴权失败", e);
        } catch (InterruptedException e) {
            logger.error("线程中断失败", e);
            Thread.currentThread().interrupt();
        } finally {
            player.stop();
        }
    }

    private static void processAudioFromMicrophone(OralChatClient oralChatClient) {
        OralChatParam param = OralChatParam.builder()
                .interactMode(StreamMode.CONTINUOUS_VAD.getValue())
                .uid("youtestuid")
                .build();

        AudioPlayer player = new AudioPlayer();
        player.start();

        MicrophoneAudioSender sender = null;

        try (Scanner scanner = new Scanner(System.in)) {

            WebSocket socket = oralChatClient.start(param, getListener(player));

            AtomicBoolean isFirst = new AtomicBoolean(true);
            // 麦克风工具类
            sender = new MicrophoneAudioSender((audioData, length) -> {
                // 发送给 WebSocket
                if (isFirst.get()) {
                    oralChatClient.sendMsg(socket, audioData, 0);
                    isFirst.set(false);
                } else {
                    oralChatClient.sendMsg(socket, audioData, 1);
                }
            });
            sender.start();

            scanner.nextLine();
            oralChatClient.stop(socket);
            TimeUnit.MILLISECONDS.sleep(3000);
        } catch (SignatureException | MalformedURLException e) {
            logger.error("API签名验证失败", e);
            throw new RuntimeException("服务鉴权失败", e);
        } catch (InterruptedException e) {
            logger.error("线程中断失败", e);
            Thread.currentThread().interrupt();
        } finally {
            player.stop();
            if (null != sender) {
                sender.stop();
            }
        }
    }

    private static WebSocketListener getListener(AudioPlayer player) {
        return new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                logger.info("websocket启动成功");
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                logger.info("接收到消息: {}", text);
            }

            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                // 处理二进制消息
                processResponse(bytes.string(StandardCharsets.UTF_8), player);
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                logger.info("websocket关闭");
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, @Nullable Response response) {
                logger.error("ws错误", t);
            }
        };
    }

    /**
     * 发送本地文件语音内容
     */
    private static void sendChat(OralChatClient oralChatClient, WebSocket socket, OralChatParam param) {
        try (RandomAccessFile raf = new RandomAccessFile(new File(resourcePath + filePath), "r")) {
            byte[] bytes = new byte[1280];
            int len;
            boolean first = true;
            while ((len = raf.read(bytes)) != -1) {
                if (len < 1280) {
                    bytes = Arrays.copyOfRange(bytes, 0, len);
                    if (first) {
                        oralChatClient.sendMsg(socket, bytes, 0);
                    } else {
                        oralChatClient.sendMsg(socket, bytes, 2);
                    }
                    break;
                }
                if (first) {
                    first = false;
                    oralChatClient.sendMsg(socket, bytes, 0);
                } else {
                    oralChatClient.sendMsg(socket, bytes, 1);
                }
                // 每隔40毫秒发送一次数据
                TimeUnit.MILLISECONDS.sleep(40);
            }
            // 发送结束标识
        } catch (Exception e) {
            logger.error("消息发送失败", e);
        }
    }

    /**
     * 处理响应数据
     */
    private static void processResponse(String message, AudioPlayer player) {
        // logger.info(message);
        try {
            JSONObject response = JSON.parseObject(message);
            // 检查响应状态
            int code = response.getJSONObject("header").getIntValue("code");
            if (0 == code) {
                JSONObject payload = response.getJSONObject("payload");
                if (null != payload) {
                    if (payload.containsKey("event")) {
                        JSONObject event = payload.getJSONObject("event");
                        String encodedText = event.getString("text");
                        String decodedText = new String(Base64.getDecoder().decode(encodedText), StandardCharsets.UTF_8);
                        logger.info("事件文本消息: {}", decodedText);
                    } else if (payload.containsKey("iat")) {
                        JSONObject iat = payload.getJSONObject("iat");
                        if (null != iat && iat.containsKey("text")) {
                            String encodedText = iat.getString("text");
                            String decodedText = new String(Base64.getDecoder().decode(encodedText), StandardCharsets.UTF_8);
                            logger.info("语音识别结果: {}", decodedText);
                        }
                    } else if (payload.containsKey("nlp")) {
                        JSONObject nlp = payload.getJSONObject("nlp");
                        if (null != nlp && nlp.containsKey("text")) {
                            String encodedText = nlp.getString("text");
                            String decodedText = new String(Base64.getDecoder().decode(encodedText), StandardCharsets.UTF_8);
                            logger.info("自然语言处理结果: {}", decodedText);
                        }
                    } else if (payload.containsKey("tts")) {
                        JSONObject tts = payload.getJSONObject("tts");
                        if (null != tts && tts.containsKey("audio")) {
                            String audioBase64 = tts.getString("audio");
                            byte[] audioData = Base64.getDecoder().decode(audioBase64);
                            player.play(audioData);
                            logger.info("接收到大小为 {}的数据", audioData.length);
                        }
                    } else if (payload.containsKey("cbm_vms")){
                        JSONObject vms = payload.getJSONObject("cbm_vms");
                        if (null != vms && vms.containsKey("text")) {
                            String encodedText = vms.getString("text");
                            byte[] decodedText = Base64.getDecoder().decode(encodedText);
                            logger.info("接收到vms数据: {}", decodedText);
                        }
                    } else {
                        logger.error("响应错误");
                    }
                }
            }
        } catch (Exception e) {
            logger.error("ws消息处理失败!");
        }
    }
}
