package cn.xfyun.demo;

import cn.xfyun.api.IatClient;
import cn.xfyun.config.PropertiesConfig;
import cn.xfyun.model.response.iat.IatResponse;
import cn.xfyun.model.response.iat.IatResult;
import cn.xfyun.service.iat.AbstractIatWebSocketListener;
import okhttp3.Response;
import okhttp3.WebSocket;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.security.SignatureException;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * IAT( iFly Auto Transform ) 语音听写
 */
public class IatClientApp {
    private static final String appId = PropertiesConfig.getAppId();
    private static final String apiKey = PropertiesConfig.getApiKey();
    private static final String apiSecret = PropertiesConfig.getApiSecret();

    private static String filePath = "audio/iat_pcm_16k.pcm";
    private static String resourcePath;

    static {
        try {
            resourcePath = IatClientApp.class.getResource("/").toURI().getPath();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) throws FileNotFoundException, SignatureException, MalformedURLException {
        IatClient iatClient = new IatClient.Builder()
                .signature(appId, apiKey, apiSecret)
                .build();

        SimpleDateFormat sdf = new SimpleDateFormat("yyy-MM-dd HH:mm:ss.SSS");
        Date dateBegin = new Date();

        File file = new File(resourcePath + filePath);
        StringBuffer finalResult = new StringBuffer();
        iatClient.send(file, new AbstractIatWebSocketListener() {
            @Override
            public void onSuccess(WebSocket webSocket, IatResponse iatResponse) {
                if (iatResponse.getCode() != 0) {
                    System.out.println("code=>" + iatResponse.getCode() + " error=>" + iatResponse.getMessage() + " sid=" + iatResponse.getSid());
                    System.out.println("错误码查询链接：https://www.xfyun.cn/document/error-code");
                    return;
                }

                if (iatResponse.getData() != null) {
                    if (iatResponse.getData().getResult() != null) {
                        IatResult.Ws[] wss = iatResponse.getData().getResult().getWs();
                        String text = "";
                        for (IatResult.Ws ws : wss) {
                            IatResult.Cw[] cws = ws.getCw();

                            for (IatResult.Cw cw : cws) {
                                text += cw.getW();
                            }
                        }

                        try {
                            finalResult.append(text);
                            System.out.println("中间识别结果 ==》" + text);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    if (iatResponse.getData().getStatus() == 2) {
                        // resp.data.status ==2 说明数据全部返回完毕，可以关闭连接，释放资源
                        System.out.println("session end ");
                        Date dateEnd = new Date();
                        System.out.println(sdf.format(dateBegin) + "开始");
                        System.out.println(sdf.format(dateEnd) + "结束");
                        System.out.println("耗时:" + (dateEnd.getTime() - dateBegin.getTime()) + "ms");
                        System.out.println("最终识别结果" + finalResult.toString());
                        System.out.println("本次识别sid ==》" + iatResponse.getSid());
                        iatClient.closeWebsocket();
                        System.exit(0);
                    } else {
                        // 根据返回的数据处理
                        //System.out.println(StringUtils.gson.toJson(iatResponse));
                    }
                }
            }

            @Override
            public void onFail(WebSocket webSocket, Throwable t, Response response) {
            }
        });
    }
}
