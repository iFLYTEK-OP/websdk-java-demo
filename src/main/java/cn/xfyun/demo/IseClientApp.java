package cn.xfyun.demo;

import cn.xfyun.api.IseClient;
import cn.xfyun.config.PropertiesConfig;
import cn.xfyun.model.response.ise.IseResponseData;
import cn.xfyun.service.ise.AbstractIseWebSocketListener;
import okhttp3.Response;
import okhttp3.WebSocket;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.security.SignatureException;
import java.util.Base64;

/**
 * ISE( iFly Speech Evaluator ) 语音评测
 * 1、APPID、APISecret、APIKey信息获取：https://console.xfyun.cn/services/ise
 * 2、文档地址：https://www.xfyun.cn/doc/Ise/IseAPI.html
 **/
public class IseClientApp {

    private static final String appId = PropertiesConfig.getAppId();
    private static final String apiKey = PropertiesConfig.getApiKey();
    private static final String apiSecret = PropertiesConfig.getApiSecret();

    private static String filePath = "audio/cn/read_sentence_cn.pcm";
    private static String resourcePath;

    static {
        try {
            resourcePath = IseClientApp.class.getResource("/").toURI().getPath();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }
    /**
     * 解码
     */
    final static Base64.Decoder decoder = Base64.getDecoder();

    public static void main(String[] args) throws FileNotFoundException, SignatureException, MalformedURLException, InterruptedException {

        IseClient client = new IseClient.Builder()
                .signature(appId, apiKey, apiSecret)
                .addSub("ise")
                .addEnt("cn_vip")
                .addCategory("read_sentence")
                .addTte("utf-8")
                .addText('\uFEFF' + "今天天气怎么样")
                .addRstcd("utf8")
                .build();

        File file = new File(resourcePath + filePath);
        client.send(file, new AbstractIseWebSocketListener() {

            @Override
            public void onSuccess(WebSocket webSocket, IseResponseData iseResponseData) {
                try {
                    System.out.println("sid:" + iseResponseData.getSid() + "\n最终识别结果:\n" + new String(decoder.decode(iseResponseData.getData().getData()), "UTF-8"));
                    System.exit(0);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFail(WebSocket webSocket, Throwable throwable, Response response) {
                System.out.println(response);
            }
        });
    }
}
