package cn.xfyun.demo;

import cn.xfyun.api.LtpClient;
import cn.xfyun.config.LtpFunctionEnum;
import cn.xfyun.config.PropertiesConfig;

/**
 * @author yingpeng
 * 自然语言处理(ltp)模块
 */
public class LtpClientApp {

    private static final String APP_ID = PropertiesConfig.getAppId();
    private static final String LTP_KEY = PropertiesConfig.getLtpKey();

    public static void main(String[] args) {
        try {
            LtpClient client = new LtpClient.Builder(APP_ID, LTP_KEY, LtpFunctionEnum.KE)
                    .build();
            String response = client.send("我来自北方");
            System.out.println("请求地址：" + client.getHostUrl());
            System.out.println(response);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println("错误码查询链接：https://www.xfyun.cn/document/error-code");
        }
    }
}
