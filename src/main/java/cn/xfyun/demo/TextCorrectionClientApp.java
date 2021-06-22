package cn.xfyun.demo;

import cn.xfyun.config.PropertiesConfig;
import cn.xfyun.api.TextCorrectionClient;

/**
 *    文本纠错
 *
 * @author mqgao
 * @version 1.0
 * @date 2021/6/22 11:07
 */
public class TextCorrectionClientApp {

	private static final String appId = PropertiesConfig.getAppId();
	private static final String apiKey = PropertiesConfig.getApiKey();
	private static final String apiSecret = PropertiesConfig.getApiSecret();


	public static void main(String[] args) throws Exception{
		TextCorrectionClient correctionClient = new TextCorrectionClient
				.Builder(appId, apiSecret, apiKey)
				.build();
		String result = correctionClient.send("画蛇天足");
		System.out.println("返回结果: " + result);
	}

}
