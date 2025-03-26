package cn.xfyun.demo.speech;

import cn.xfyun.api.IgrClient;
import cn.xfyun.config.PropertiesConfig;
import cn.xfyun.model.response.igr.IgrResponseData;
import cn.xfyun.service.igr.AbstractIgrWebSocketListener;
import okhttp3.Response;
import okhttp3.WebSocket;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.security.SignatureException;

/**
 * @author: <flhong2@iflytek.com>
 * @description: 性别年龄识别
 * @version: v1.0
 * @create: 2021-06-11 09:51
 **/
public class IgrClientApp {

    private static final String appId = PropertiesConfig.getAppId();
    private static final String apiKey = PropertiesConfig.getApiKey();
    private static final String apiSecret = PropertiesConfig.getApiSecret();

    private static String filePath = "audio/cn/read_sentence_cn.pcm";
    private static String resourcePath;

    static {
        try {
            resourcePath = IgrClientApp.class.getResource("/").toURI().getPath();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws FileNotFoundException, SignatureException, MalformedURLException, InterruptedException {
        IgrClient igrClient = new IgrClient.Builder()
                .signature(appId, apiKey, apiSecret).ent("igr").aue("raw").rate(8000)
                .build();

        File file = new File(resourcePath + filePath);
        igrClient.send(file, new AbstractIgrWebSocketListener() {
            @Override
            public void onSuccess(WebSocket webSocket, IgrResponseData igrResponseData) {
                System.out.println("sid:" + igrResponseData.getSid());
                System.out.println("识别结果为: " + igrResponseData.getData());
                webSocket.close(1000, "");
                System.exit(0);
            }

            @Override
            public void onFail(WebSocket webSocket, Throwable t, Response response) {

            }
        });

    }
}
