package cn.xfyun.demo;

import cn.xfyun.api.SparkIatClient;
import cn.xfyun.config.PropertiesConfig;
import cn.xfyun.config.SparkIatModelEnum;
import cn.xfyun.model.sparkiat.SparkIatResponse;
import cn.xfyun.service.sparkiat.AbstractSparkIatWebSocketListener;
import cn.xfyun.util.StringUtils;
import okhttp3.Response;
import okhttp3.WebSocket;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.SignatureException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.List;


/**
 * SPARK_ZH_IAT( iFly Spark Auto Transform ) 方言大模型语音听写
 * 1、APPID、APISecret、APIKey信息获取：https://console.xfyun.cn/services/iat_zh_cn_mulacc_slm
 * 2、文档地址：https://www.xfyun.cn/doc/spark/spark_slm_iat.html
 */
public class SparkIatMulZhClientApp {
    private static final String appId = PropertiesConfig.getAppId();
    private static final String apiKey = PropertiesConfig.getApiKey();
    private static final String apiSecret = PropertiesConfig.getApiSecret();

    private static String filePath = "audio/spark_iat_mul_cn_16k_10.pcm";
    private static String resourcePath;

    static {
        try {
            resourcePath = SparkIatMulZhClientApp.class.getResource("/").toURI().getPath();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) throws FileNotFoundException, SignatureException, MalformedURLException {
        SparkIatClient sparkIatClient = new SparkIatClient.Builder()
                .mulLanguage(SparkIatModelEnum.ZH_CN_MULACC.getCode())
                .signature(appId, apiKey, apiSecret)
                .build();

        SimpleDateFormat sdf = new SimpleDateFormat("yyy-MM-dd HH:mm:ss.SSS");
        Date dateBegin = new Date();

        File file = new File(resourcePath + filePath);
        StringBuffer finalResult = new StringBuffer();
        sparkIatClient.send(file, new AbstractSparkIatWebSocketListener() {
            @Override
            public void onSuccess(WebSocket webSocket, SparkIatResponse resp) {
                if (resp.getHeader().getCode() != 0) {
                    System.out.println("code=>" + resp.getHeader().getCode() + " error=>" + resp.getHeader().getMessage() + " sid=" +resp.getHeader().getSid());
                    System.out.println("错误码查询链接：https://www.xfyun.cn/document/error-code");
                    return;
                }

                if (resp.getPayload() != null) {
                    if (resp.getPayload().getResult() != null) {
                        String tansTxt = resp.getPayload().getResult().getText();
                        if (null != tansTxt) {
                            //解码转写结果
                            byte[] decodedBytes = Base64.getDecoder().decode(resp.getPayload().getResult().getText());
                            String decodeRes = new String(decodedBytes, StandardCharsets.UTF_8);
                            SparkIatResponse.JsonParseText jsonParseText = StringUtils.gson.fromJson(decodeRes, SparkIatResponse.JsonParseText.class);

                            String text = "";
                            for (SparkIatResponse.Ws ws : jsonParseText.getWs()) {
                                List<SparkIatResponse.Cw> cwList = ws.getCw();

                                for (SparkIatResponse.Cw cw : cwList) {
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
                    }

                    //流式实时返回结果处理方式
                    /*if (null != resp.getPayload().getResult().getText()) {
                        byte[] decodedBytes = Base64.getDecoder().decode(resp.getPayload().getResult().getText());
                        String decodeRes = new String(decodedBytes, StandardCharsets.UTF_8);
//                        System.out.println("中间识别结果 ==》" + decodeRes);
                        SparkIatResponse.JsonParseText jsonParseText = StringUtils.gson.fromJson(decodeRes, SparkIatResponse.JsonParseText.class);

                        String mark = "";
                        if (jsonParseText.getPgs().equals("apd")) {
                            mark = "结果追加到上面结果";
                        } else if (jsonParseText.getPgs().equals("rpl")) {
                            mark = "结果替换前面" + jsonParseText.getRg().get(0) + "到" + jsonParseText.getRg().get(1);
                        }
                        System.out.print("中间识别结果 【" + mark + "】==》");

                        List<SparkIatResponse.Ws> wsList = jsonParseText.getWs();
                        for (SparkIatResponse.Ws ws : wsList) {
                            List<SparkIatResponse.Cw> cwList = ws.getCw();
                            for (SparkIatResponse.Cw cw : cwList) {
                                System.out.print(cw.getW());
                            }
                        }
                    }*/

                    if (resp.getPayload().getResult().getStatus() == 2) {
                        // resp.data.status ==2 说明数据全部返回完毕，可以关闭连接，释放资源
                        System.out.println("session end ");
                        Date dateEnd = new Date();
                        System.out.println(sdf.format(dateBegin) + "开始");
                        System.out.println(sdf.format(dateEnd) + "结束");
                        System.out.println("耗时:" + (dateEnd.getTime() - dateBegin.getTime()) + "ms");
                        System.out.println("最终识别结果 ==》" + finalResult.toString());
                        System.out.println("本次识别sid ==》" + resp.getHeader().getSid());
                        sparkIatClient.closeWebsocket();
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
